package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.util.VibrancyThreadPool
import org.lwjgl.system.NativeResource
import java.util.concurrent.CompletableFuture

open class SkyLightMeshManager(
    @JvmField
    val blit: (manager: SkyLightMeshManager) -> Unit
) : NativeResource {
    @JvmField
    val lightMesh = LightMesh(GlBufferUsage.STATIC_DRAW)
    @JvmField
    val shadowBuffer = ShadowBuffer(GlBufferUsage.STATIC_DRAW)
    @JvmField
    protected var asyncTask: CompletableFuture<() -> Unit>? = null

    override fun free() {
        lightMesh.free()
        shadowBuffer.free()
        asyncTask?.cancel(true)
    }

    fun draw() {
        lightMesh.draw()
    }

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(): Boolean {
        asyncTask?.let { task ->
            try {
                if (task.isDone) {
                    task.get()()

                    if (!lightMesh.empty) {
                        blit(this)
                    }

                    asyncTask = null
                    return true
                }
            } catch (e: NullPointerException) {
                asyncTask = null
            }
        }

        return false
    }

    protected fun rebuildBlocksAsyncImpl(
        manager: LightManager,
        collector: BlockMeshCollector,
        shadowPredicate: BlockMeshCollector.Predicate,
        lightPredicate: BlockMeshCollector.Predicate
    ): () -> Unit {
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

        val shadows = shadowBuffer.lazyUpload(NeoAtlas.blocks.width, NeoAtlas.blocks.height, shadowFaces)
        val light = lightMesh.lazyUploadNoAtlas(lightFaces)

        return {
            shadows()
            light()
        }
    }

    fun rebuildBlocksAsync(
        manager: LightManager,
        collector: BlockMeshCollector,
        shadowPredicate: BlockMeshCollector.Predicate,
        lightPredicate: BlockMeshCollector.Predicate
    ) {
        if (VibrancyConfig.useMultithreading) {
            asyncTask?.cancel(true)
            asyncTask = CompletableFuture.supplyAsync({ rebuildBlocksAsyncImpl(manager, collector, shadowPredicate, lightPredicate) }, VibrancyThreadPool)
        } else {
            rebuildBlocksAsyncImpl(manager, collector, shadowPredicate, lightPredicate)()

            if (!lightMesh.empty) {
                blit(this)
            }
        }
    }
}