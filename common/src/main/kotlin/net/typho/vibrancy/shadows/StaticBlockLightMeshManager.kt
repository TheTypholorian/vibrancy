package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.util.VibrancyThreadPool
import org.lwjgl.system.NativeResource
import java.util.concurrent.CompletableFuture

open class StaticBlockLightMeshManager(
    @JvmField
    val blit: (manager: StaticBlockLightMeshManager, info: LightMesh.MeshData) -> Unit
) : NativeResource {
    @JvmField
    val lightMesh = LightMesh(GlBufferUsage.STATIC_DRAW)
    @JvmField
    val shadowBuffer = ShadowBuffer(GlBufferUsage.STATIC_DRAW)
    @JvmField
    protected var asyncTask: CompletableFuture<() -> LightMesh.MeshData>? = null

    override fun free() {
        lightMesh.free()
        shadowBuffer.free()
        asyncTask?.cancel(true)
    }

    fun draw(shader: GlBoundProgram) {
        lightMesh.draw(shader)
    }

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(): Boolean {
        asyncTask?.let { task ->
            if (task.isDone) {
                val info = task.get()()

                if (!lightMesh.empty) {
                    blit(this, info)
                }

                asyncTask = null
                return true
            }
        }

        return false
    }

    protected fun rebuildBlocksAsyncImpl(
        manager: LightManager,
        collector: BlockMeshCollector,
        shadowPredicate: BlockMeshCollector.Predicate,
        lightPredicate: BlockMeshCollector.Predicate
    ): () -> LightMesh.MeshData {
        val level = manager.getLevel() ?: throw NullPointerException("No level?")

        val shadowFaces = arrayListOf<LightFace>()
        val lightFaces = arrayListOf<LightFace>()
        collector.submit(
            manager,
            level,
            NeoAtlas.blocks,
            object : BlockMeshCollector.Consumer {
                override val predicate: BlockMeshCollector.Predicate = shadowPredicate

                override fun collect(faces: Iterable<LightFace>) {
                    shadowFaces.addAll(faces)
                }
            },
            object : BlockMeshCollector.Consumer {
                override val predicate: BlockMeshCollector.Predicate = lightPredicate

                override fun collect(faces: Iterable<LightFace>) {
                    lightFaces.addAll(faces)
                }
            }
        )

        val shadows = shadowBuffer.lazyUpload(shadowFaces)
        val light = lightMesh.lazyUpload(lightFaces)

        return {
            shadows()
            LightMesh.MeshData(
                lightFaces,
                light()
            )
        }
    }

    fun rebuildBlocksAsync(
        manager: LightManager,
        collector: BlockMeshCollector,
        shadowPredicate: BlockMeshCollector.Predicate,
        lightPredicate: BlockMeshCollector.Predicate
    ) {
        if (VibrancyConfig().useMultithreading) {
            /*
            GlThreadPool.submit {
                val info = rebuildAsyncImpl(manager, collector, shadowPredicate, lightPredicate)()

                if (!lightMesh.empty) {
                    blit(info)
                }
            }
             */
            asyncTask?.cancel(true)
            asyncTask = CompletableFuture.supplyAsync({ rebuildBlocksAsyncImpl(manager, collector, shadowPredicate, lightPredicate) }, VibrancyThreadPool)
        } else {
            val info = rebuildBlocksAsyncImpl(manager, collector, shadowPredicate, lightPredicate)()

            if (!lightMesh.empty) {
                blit(this, info)
            }
        }
    }
}