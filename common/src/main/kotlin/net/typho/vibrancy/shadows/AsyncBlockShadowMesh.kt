package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.util.GlThreadPool

open class AsyncBlockShadowMesh<M : ShadowMesher>(
    @JvmField
    val mesher: M,
    @JvmField
    val blit: (info: LightMesh.LightBlitInfo) -> Unit
) : ShadowMesh() {
    //protected var asyncTask: CompletableFuture<() -> LightMesh.LightBlitInfo>? = null

    fun isTaskActive() = false//asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(): Boolean {
        /*
        asyncTask?.let { task ->
            if (task.isDone) {
                val info = task.get()()

                if (!lightMesh.empty) {
                    blit(info)
                }

                asyncTask = null
                return true
            }
        }
         */

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
        predicate: LightFacePredicate,
        shadowPredicate: (face: LightFace) -> Boolean
    ) {
        if (Vibrancy.config.useMultithreading) {
            GlThreadPool.submit {
                rebuildAsyncImpl(manager, predicate, shadowPredicate)()
            }
            //asyncTask?.cancel(true)
            //asyncTask = CompletableFuture.supplyAsync { rebuildAsyncImpl(manager, predicate, shadowPredicate) }
        } else {
            val info = rebuildAsyncImpl(manager, predicate, shadowPredicate)()

            if (!lightMesh.empty) {
                blit(info)
            }
        }
    }
}