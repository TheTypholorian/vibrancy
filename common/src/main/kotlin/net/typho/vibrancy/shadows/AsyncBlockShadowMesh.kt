package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
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

                if (!lightMesh.value!!.empty) {
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
        predicate: ShadowPredicate
    ): () -> LightMesh.LightBlitInfo {
        val level = manager.getLevel() ?: throw NullPointerException("No level?")

        val shadowFaces = arrayListOf<LightFace>()
        val lightFaces = arrayListOf<LightFace>()
        synchronized(mesher) {
            mesher.submit(manager, level, predicate, TextureUtil.INSTANCE.blockAtlas, shadowFaces::add, lightFaces::add)
        }

        val built = build(level, shadowFaces, lightFaces)

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
        predicate: ShadowPredicate
    ) {
        if (Vibrancy.config.useMultithreading) {
            asyncTask?.cancel(true)
            asyncTask = CompletableFuture.supplyAsync { rebuildAsyncImpl(manager, predicate) }
        } else {
            val info = rebuildAsyncImpl(manager, predicate)()

            if (!lightMesh.value!!.empty) {
                blit(info, data)
            }
        }
    }
}