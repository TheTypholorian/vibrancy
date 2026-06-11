package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.iterator
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.util.GlTask
import net.typho.vibrancy.util.SectionMeshCache
import net.typho.vibrancy.util.VibrancyThreadPool
import org.lwjgl.system.NativeResource

open class StaticOneStepBlockLightMeshManager(
    @JvmField
    val pos: IVec3<Int>,
    @JvmField
    val bounds: (manager: StaticOneStepBlockLightMeshManager) -> AbstractRect3<Int>,
    @JvmField
    val blit: (manager: StaticOneStepBlockLightMeshManager, info: LightMesh.ComplexMeshData, profiler: ProfilerFiller) -> Unit
) : NativeResource {
    @JvmField
    val lightMesh = LightMesh(GlBufferUsage.STATIC_DRAW)
    @JvmField
    val gridBuffer = VoxelGridBuffer(GlBufferUsage.STATIC_DRAW)
    @JvmField
    var blockEntities: List<BlockPos>? = null
    @JvmField
    protected var meshTask: GlTask<LightMesh.ComplexMeshData?>? = null
    var shouldMesh = true
        protected set

    fun getBlockEntities(level: Level): List<BlockPos> {
        blockEntities?.let { return it }

        val list = arrayListOf<BlockPos>()

        bounds(this).iterator().forEach { pos ->
            val block = (pos + this.pos).blockPos

            if (level.getBlockEntity(block) != null) {
                list.add(block)
            }
        }

        blockEntities = list
        return list
    }

    fun queueMesh() {
        shouldMesh = true
    }

    override fun free() {
        lightMesh.free()
        gridBuffer.free()
        meshTask?.cancel()
    }

    fun numActiveTasks(): Int = meshTask?.let { task -> if (task.isDone) 0 else 1 } ?: 0

    fun tick(
        data: RenderEventData,
        manager: LightManager,
        profiler: ProfilerFiller
    ) {
        meshTask?.let { task ->
            if (task.isDoneOrCancelled()) {
                try {
                    profiler.push("finish")
                    task.finish()?.let {
                        if (!lightMesh.empty) {
                            blit(this, it, profiler)
                        }
                    }
                    profiler.pop()
                } catch (e: Exception) {
                    Vibrancy.LOGGER.warn("Error finishing block light mesh task", e)
                }

                meshTask = null
            }
        }

        if (shouldMesh) {
            profiler.push("start")
            mesh(data, manager, profiler)
            //shouldMesh = false
            profiler.pop()
        }
    }

    protected fun meshImpl(
        isCancelled: () -> Boolean,
        manager: LightManager
    ): Pair<AutoCloseable, () -> LightMesh.ComplexMeshData?> {
        val sectionMeshes = hashMapOf<SectionPos, SectionMeshCache?>()
        var numFaces = 0
        val bounds = bounds(this)
        val faces = arrayListOf<Pair<IVec3<Int>, List<LightFace>>>()

        bounds.iterator().forEach { pos ->
            val ax = pos.x + this.pos.x
            val ay = pos.y + this.pos.y
            val az = pos.z + this.pos.z

            val sectionPos = SectionPos.of(
                SectionPos.blockToSectionCoord(ax),
                SectionPos.blockToSectionCoord(ay),
                SectionPos.blockToSectionCoord(az)
            )
            sectionMeshes.computeIfAbsent(sectionPos) { key -> synchronized(manager.sectionLock) { manager.sectionMeshCaches[key] } }?.let { section ->
                section.get(ax, ay, az)?.let { block ->
                    val list = arrayListOf<LightFace>()
                    block.collect { list.add(it.copyWithOffset(pos.x, pos.y, pos.z)) }
                    numFaces += list.size
                    faces.add(pos to list)
                }
            }
        }

        if (isCancelled()) {
            return AutoCloseable { } to { null }
        }

        val shadows = gridBuffer.lazyUpload(NeoAtlas.blocks.width, NeoAtlas.blocks.height, numFaces, bounds, faces)

        if (isCancelled()) {
            return shadows.first to { null }
        }

        val light = lightMesh.lazyUpload(faces, numFaces)

        return AutoCloseable {
            shadows.first.close()
            light.first.close()
        } to {
            blockEntities = null
            shadows.second()
            LightMesh.ComplexMeshData(
                faces,
                numFaces,
                light.second()
            )
        }
    }

    fun mesh(
        data: RenderEventData,
        manager: LightManager,
        profiler: ProfilerFiller
    ) {
        if (VibrancyConfig.useMultithreading) {
            meshTask?.cancel()
            meshTask = VibrancyThreadPool.submit(data, pos, manager) { meshImpl(it, manager) }
        } else {
            val result = meshImpl({ false }, manager)
            result.second()?.let {
                if (!lightMesh.empty) {
                    blit(this, it, profiler)
                }
            }
            result.first.close()
        }
    }
}