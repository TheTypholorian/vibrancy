package net.typho.vibrancy.shadows

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

open class StaticBlockLightMeshManager(
    @JvmField
    val blit: (manager: StaticBlockLightMeshManager, info: LightMesh.MeshData) -> Unit
) : NativeResource {
    @JvmField
    val lightMesh = LightMesh(GlBufferUsage.STATIC_DRAW)
    @JvmField
    val shadowBuffer = ShadowBuffer(GlBufferUsage.STATIC_DRAW)
    @JvmField
    protected var asyncTask: GlTask<LightMesh.MeshData?>? = null

    override fun free() {
        lightMesh.free()
        shadowBuffer.free()
        asyncTask?.cancel()
    }

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(): Boolean {
        asyncTask?.let { task ->
            if (task.isDoneOrCancelled()) {
                try {
                    task.finish()?.let {
                        if (!lightMesh.empty) {
                            blit(this, it)
                        }
                    }
                } catch (e: NullPointerException) {
                    Vibrancy.LOGGER.warn("Error finishing block light task", e)
                }

                asyncTask = null
                return true
            }
        }

        return false
    }

    protected fun rebuildBlocksAsyncImpl(
        isCancelled: () -> Boolean,
        manager: LightManager,
        collector: BlockMeshCollector,
        shadowPredicate: BlockMeshCollector.Predicate,
        lightPredicate: BlockMeshCollector.Predicate
    ): Pair<AutoCloseable, () -> LightMesh.MeshData?> {
        val level = manager.getLevel() ?: throw NullPointerException("No level?")

        val shadowFaces = arrayListOf<LightFace>()
        val lightFaces = arrayListOf<LightFace>()
        if (!collector.submit(
            isCancelled,
            manager,
            level,
            NeoAtlas.blocks,
            object : BlockMeshCollector.Consumer {
                override val predicate: BlockMeshCollector.Predicate = shadowPredicate

                override fun collect(faces: Iterable<LightFace>, origin: BlockMeshCollector.FaceOrigin) {
                    shadowFaces.addAll(faces)
                }
            },
            object : BlockMeshCollector.Consumer {
                override val predicate: BlockMeshCollector.Predicate = lightPredicate

                override fun collect(faces: Iterable<LightFace>, origin: BlockMeshCollector.FaceOrigin) {
                    lightFaces.addAll(faces)
                }
            }
        )) {
            return AutoCloseable { } to { null }
        }

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
            LightMesh.MeshData(
                lightFaces,
                light.second()
            )
        }
    }

    fun rebuildBlocksAsync(
        data: RenderEventData,
        pos: IVec3<Int>,
        manager: LightManager,
        collector: BlockMeshCollector,
        shadowPredicate: BlockMeshCollector.Predicate,
        lightPredicate: BlockMeshCollector.Predicate
    ) {
        if (VibrancyConfig.useMultithreading) {
            asyncTask?.cancel()
            asyncTask = VibrancyThreadPool.submit(data, pos, manager) { rebuildBlocksAsyncImpl(it, manager, collector, shadowPredicate, lightPredicate) }
        } else {
            val result = rebuildBlocksAsyncImpl({ false }, manager, collector, shadowPredicate, lightPredicate)
            result.second()?.let {
                if (!lightMesh.empty) {
                    blit(this, it)
                }
            }
            result.first.close()
        }
    }
}