package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import java.util.concurrent.CompletableFuture

open class AsyncBlockShadowMesh<M : ShadowMesher>(
    @JvmField
    val mesher: M,
    @JvmField
    val blit: (info: LightMesh.LightBlitInfo, data: RenderEventData) -> Unit
) : ShadowMesh() {
    protected var asyncTask: CompletableFuture<() -> LightMesh.LightBlitInfo>? = null

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(data: RenderEventData): Boolean {
        asyncTask?.let { task ->
            if (task.isDone) {
                val info = task.get()()

                if (!lightMesh.empty) {
                    blit(info, data)
                }

                asyncTask = null
                return true
            }
        }

        return false
    }

    protected fun rebuildAsyncImpl(
        manager: LightManager,
        predicate: LightFacePredicate,
        shadowPredicate: (face: LightFace) -> Boolean
    ): () -> LightMesh.LightBlitInfo {
        val level = manager.getLevel() ?: throw NullPointerException("No level?")

        val shadowFaces = arrayListOf<LightFace>()
        val lightFaces = arrayListOf<LightFace>()
        synchronized(mesher) {
            mesher.submit(manager, level, NeoAtlas.blocks, predicate) { face ->
                if (shadowPredicate(face)) {
                    shadowFaces.add(face)
                }

                lightFaces.add(face)
            }
        }

        val built = build(shadowFaces, lightFaces)

        return {
            LightMesh.LightBlitInfo(
                built(),
                lightFaces
            )
        }
    }

    fun rebuildAsync(
        manager: LightManager,
        data: RenderEventData,
        predicate: LightFacePredicate,
        shadowPredicate: (face: LightFace) -> Boolean
    ) {
        if (Vibrancy.config.useMultithreading) {
            asyncTask?.cancel(true)
            asyncTask = CompletableFuture.supplyAsync { rebuildAsyncImpl(manager, predicate, shadowPredicate) }
        } else {
            val info = rebuildAsyncImpl(manager, predicate, shadowPredicate)()

            if (!lightMesh.empty) {
                blit(info, data)
            }
        }
    }
}