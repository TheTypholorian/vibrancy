package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.util.profiling.ProfilerFiller
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.iterator
import net.typho.big_shot_lib.api.math.vec.IVec3
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
    val shadowBuffer = ShadowBuffer.VoxelGrid(GlBufferUsage.STATIC_DRAW)
    @JvmField
    var blockEntities = arrayListOf<BlockPos>()
    @JvmField
    protected var meshTask: GlTask<LightMesh.ComplexMeshData?>? = null
    var shouldMesh = true
        protected set

    fun queueMesh() {
        shouldMesh = true
    }

    override fun free() {
        lightMesh.free()
        shadowBuffer.free()
        meshTask?.cancel()
    }

    fun numActiveTasks(): Int = meshTask?.let { task -> if (task.isDone) 0 else 1 } ?: 0

    fun tick(
        data: RenderEventData,
        pos: IVec3<Int>,
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
            mesh(data, pos, manager, profiler)
            shouldMesh = false
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
        val blockEntities = arrayListOf<BlockPos>()
        val faces = arrayListOf<Pair<IVec3<Int>, List<LightFace>>>()

        for (pos in bounds) {
            val ax = pos.x + this.pos.x
            val ay = pos.y + this.pos.y
            val az = pos.z + this.pos.z
            val blockPos = BlockPos(ax, ay, az)

            val sectionPos = SectionPos.of(
                SectionPos.blockToSectionCoord(ax),
                SectionPos.blockToSectionCoord(ay),
                SectionPos.blockToSectionCoord(az)
            )
            sectionMeshes.computeIfAbsent(sectionPos, manager.sectionMeshCaches::get)?.let { section ->
                section.get(ax, ay, az)?.let { block ->
                    val list = arrayListOf<LightFace>()
                    block.solidFaces.mapTo(list) { it.copyWithOffset(pos.x, pos.y, pos.z) } // TODO split solid and translucent
                    block.translucentFaces.mapTo(list) { it.copyWithOffset(pos.x, pos.y, pos.z) }
                    numFaces += list.size
                    faces.add(pos to list)
                }
            }

            if (manager.getLevel()!!.getBlockEntity(blockPos) != null) {
                blockEntities.add(blockPos)
            }
        }

        if (isCancelled()) {
            return AutoCloseable { } to { null }
        }

        val shadows = shadowBuffer.lazyUpload(NeoAtlas.blocks.width, NeoAtlas.blocks.height, numFaces, bounds, faces)

        if (isCancelled()) {
            return shadows.first to { null }
        }

        val light = lightMesh.lazyUpload(faces, numFaces)

        return AutoCloseable {
            shadows.first.close()
            light.first.close()
        } to {
            this.blockEntities = blockEntities
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
        pos: IVec3<Int>,
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