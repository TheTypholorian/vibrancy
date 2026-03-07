package net.typho.vibrancy.shadows

import net.typho.vibrancy.LightManager
import net.typho.vibrancy.util.PointLight
import java.util.*
import java.util.concurrent.CompletableFuture

open class AsyncBlockShadowMesh : ShadowMesh() {
    protected var asyncTask: CompletableFuture<Runnable>? = null

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(): Boolean {
        asyncTask?.let { task ->
            if (task.isDone) {
                task.get().run()

                asyncTask = null
                return true
            }
        }

        return false
    }

    fun rebuildAsync(
        manager: LightManager,
        mesher: ShadowMesher,
        light: PointLight
    ) {
        rebuildAsync(
            manager,
            mesher,
            light.shadowPredicate!!
        )
    }

    fun rebuildAsync(
        manager: LightManager,
        mesher: ShadowMesher,
        predicate: ShadowPredicate
    ) {
        asyncTask?.cancel(true)
        asyncTask = CompletableFuture.supplyAsync {
            val level = manager.getLevel() ?: throw NullPointerException("No level?")

            val shadowFaces = LinkedList<LightFace>()
            val lightFaces = LinkedList<LightFace>()
            mesher.submit(manager, level, predicate, shadowFaces::add, lightFaces::add)

            val task = build(level, shadowFaces, lightFaces)

            return@supplyAsync task
        }
    }
}