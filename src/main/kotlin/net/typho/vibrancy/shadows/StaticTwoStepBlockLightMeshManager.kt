package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.util.GlTask
import net.typho.vibrancy.util.VibrancyThreadPool
import org.lwjgl.system.NativeResource

open class StaticTwoStepBlockLightMeshManager<C : BlockMeshCollector>(
    @JvmField
    val lightPredicate: BlockMeshCollector.Predicate,
    @JvmField
    val shadowPredicate: BlockMeshCollector.Predicate,
    @JvmField
    val collector: C,
    @JvmField
    val pos: IVec3<Int>,
    @JvmField
    val blit: (manager: StaticTwoStepBlockLightMeshManager<C>, info: LightMesh.FlatMeshData) -> Unit
) : NativeResource {
    @JvmField
    val lightMesh = LightMesh(GlBufferUsage.STATIC_DRAW)
    @JvmField
    val shadowBuffer = ShadowBuffer(GlBufferUsage.STATIC_DRAW)
    @JvmField
    protected var scanTask: GlTask<Unit>? = null
    @JvmField
    protected var meshTask: GlTask<LightMesh.FlatMeshData?>? = null
    var shouldScan = true
        protected set
    var shouldMesh = true
        protected set

    fun queueScan() {
        shouldScan = true
        shouldMesh = true
    }

    fun queueScan(level: Level, pos: BlockPos, stateChange: Pair<BlockState, BlockState>) {
        if (lightPredicate.requiresScan(level, pos, stateChange.first, stateChange.second) || shadowPredicate.requiresScan(level, pos, stateChange.first, stateChange.second)) {
            queueScan()
        }
    }

    fun queueMesh() {
        shouldMesh = true
    }

    override fun free() {
        lightMesh.free()
        shadowBuffer.free()
        scanTask?.cancel()
        meshTask?.cancel()
    }

    fun numActiveTasks(): Int = (scanTask?.let { task -> if (task.isDone) 0 else 1 } ?: 0) + (meshTask?.let { task -> if (task.isDone) 0 else 1 } ?: 0)

    fun tick(
        data: RenderEventData,
        pos: IVec3<Int>,
        manager: LightManager
    ) {
        meshTask?.let { task ->
            if (task.isDoneOrCancelled()) {
                try {
                    task.finish()?.let {
                        if (!lightMesh.empty) {
                            blit(this, it)
                        }
                    }
                } catch (e: Exception) {
                    Vibrancy.LOGGER.warn("Error finishing block light mesh task", e)
                }

                meshTask = null
            }
        }
        scanTask?.let { task ->
            if (task.isDoneOrCancelled()) {
                try {
                    task.finish()

                    if (shouldMesh) {
                        mesh(data, pos, manager)
                        shouldMesh = false
                    }
                } catch (e: Exception) {
                    Vibrancy.LOGGER.warn("Error finishing block light scan task", e)
                }

                scanTask = null
            }
        }

        if (shouldScan) {
            scan(data, pos, manager)
            meshTask?.cancel()
            shouldScan = false
        } else if (scanTask == null && shouldMesh) {
            mesh(data, pos, manager)
            shouldMesh = false
        }
    }

    protected fun scanImpl(
        isCancelled: () -> Boolean,
        manager: LightManager
    ) {
        collector.scan(
            isCancelled,
            manager,
            manager.getLevel() ?: throw NullPointerException("No level?"),
            lightPredicate or shadowPredicate
        )
    }

    fun scan(
        data: RenderEventData,
        pos: IVec3<Int>,
        manager: LightManager
    ) {
        if (VibrancyConfig.useMultithreading) {
            scanTask?.cancel()
            scanTask = VibrancyThreadPool.submitClean(data, pos, manager) { scanImpl(it, manager) }
        } else {
            scanImpl({ false }, manager)
        }
    }

    protected fun meshImpl(
        isCancelled: () -> Boolean,
        manager: LightManager
    ): Pair<AutoCloseable, () -> LightMesh.FlatMeshData?> {
        val level = manager.getLevel() ?: throw NullPointerException("No level?")

        val shadowFaces = arrayListOf<PrimitiveQuad>()
        val lightFaces = arrayListOf<LightFace>()
        collector.mesh(
            isCancelled,
            manager,
            level,
            object : BlockMeshCollector.Consumer {
                override fun collect(
                    faces: Iterable<LightFace>,
                    section: SectionPos,
                    block: BlockPos,
                    translucent: Boolean
                ) {
                    val state = level.getBlockState(block)

                    if (shadowPredicate.shouldCastBlock(level, block, state)) {
                        faces.filterTo(shadowFaces) { shadowPredicate.shouldCastFace(level, block, state, it) }
                    }

                    if (lightPredicate.shouldCastBlock(level, block, state)) {
                        faces.filterTo(lightFaces) { lightPredicate.shouldCastFace(level, block, state, it) }
                    }
                }
            }
        )

        if (isCancelled()) {
            return AutoCloseable { } to { null }
        }

        val shadows = shadowBuffer.lazyUpload(NeoAtlas.blocks.width, NeoAtlas.blocks.height, shadowFaces)

        if (isCancelled()) {
            return shadows.first to { null }
        }

        val light = lightMesh.lazyUpload(lightFaces)

        return AutoCloseable {
            shadows.first.close()
            light.first.close()
        } to {
            shadows.second()
            LightMesh.FlatMeshData(
                lightFaces,
                light.second()
            )
        }
    }

    fun mesh(
        data: RenderEventData,
        pos: IVec3<Int>,
        manager: LightManager
    ) {
        if (VibrancyConfig.useMultithreading) {
            meshTask?.cancel()
            meshTask = VibrancyThreadPool.submit(data, pos, manager) { meshImpl(it, manager) }
        } else {
            val result = meshImpl({ false }, manager)
            result.second()?.let {
                if (!lightMesh.empty) {
                    blit(this, it)
                }
            }
            result.first.close()
        }
    }
}