package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import java.util.*
import java.util.concurrent.CompletableFuture

open class AsyncBlockShadowMesh<M : ShadowMesher>(
    @JvmField
    val mesher: M,
    @JvmField
    val blit: (data: RenderEventData) -> Unit
) : ShadowMesh() {
    protected var asyncTask: CompletableFuture<Runnable>? = null

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(data: RenderEventData): Boolean {
        asyncTask?.let { task ->
            if (task.isDone) {
                task.get().run()
                blit(data)

                asyncTask = null
                return true
            }
        }

        return false
    }

    protected fun rebuildAsyncImpl(
        manager: LightManager,
        predicate: ShadowPredicate
    ): Runnable {
        val level = manager.getLevel() ?: throw NullPointerException("No level?")

        val shadowFaces = LinkedList<LightFace>()
        val lightFaces = LinkedList<LightFace>()
        synchronized(mesher) {
            mesher.submit(manager, level, predicate, shadowFaces::add, lightFaces::add)
        }

        return build(level, shadowFaces, lightFaces)
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
            rebuildAsyncImpl(manager, predicate).run()
            blit(data)
        }
    }
}