package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.util.GlTask
import net.typho.vibrancy.util.Offset3DArray
import net.typho.vibrancy.util.SectionMeshCache
import net.typho.vibrancy.util.VibrancyThreadPool
import org.lwjgl.system.NativeResource

open class StaticOneStepBlockLightMeshManager(
    @JvmField
    val pos: IVec3<Int>,
    @JvmField
    val bounds: (manager: StaticOneStepBlockLightMeshManager) -> AbstractRect3<Int>,
    @JvmField
    val blit: (manager: StaticOneStepBlockLightMeshManager, info: LightMesh.ComplexMeshData) -> Unit
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

        if (shouldMesh) {
            mesh(data, pos, manager)
            shouldMesh = false
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
        val faces = Offset3DArray(bounds, Offset3DArray.FlatInitializer<MutableList<LightFace>?> { x, y, z ->
            val ax = x + bounds.min.x + pos.x
            val ay = y + bounds.min.y + pos.y
            val az = z + bounds.min.z + pos.z
            val block = BlockPos(ax, ay, az)

            val rx = x + bounds.min.x
            val ry = y + bounds.min.y
            val rz = z + bounds.min.z

            val pos = SectionPos.of(
                SectionPos.blockToSectionCoord(ax),
                SectionPos.blockToSectionCoord(ay),
                SectionPos.blockToSectionCoord(az)
            )
            sectionMeshes.computeIfAbsent(pos, manager.sectionMeshCaches::get)?.let { section ->
                section.get(ax, ay, az)?.let { block ->
                    val list = arrayListOf<LightFace>()
                    block.solidFaces.mapTo(list) { it.copyWithOffset(rx, ry, rz) } // TODO split solid and translucent
                    block.translucentFaces.mapTo(list) { it.copyWithOffset(rx, ry, rz) }
                    numFaces += list.size
                    return@FlatInitializer list
                }
            }

            if (manager.getLevel()!!.getBlockEntity(block) != null) {
                blockEntities.add(block)
            }

            return@FlatInitializer null
        })

        if (isCancelled()) {
            return AutoCloseable { } to { null }
        }

        val shadows = shadowBuffer.lazyUpload(NeoAtlas.blocks.width, NeoAtlas.blocks.height, numFaces, faces)

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