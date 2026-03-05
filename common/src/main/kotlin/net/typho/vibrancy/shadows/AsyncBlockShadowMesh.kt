package net.typho.vibrancy.shadows

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy.toBlockBox
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
            light.blockPos,
            light.boundingBox.toBlockBox(),
            light.shadowPredicate!!
        )
    }

    fun rebuildAsync(
        manager: LightManager,
        mesher: ShadowMesher,
        origin: BlockPos?,
        box: BlockBox,
        predicate: ShadowPredicate
    ) {
        asyncTask?.cancel(true)
        asyncTask = CompletableFuture.supplyAsync {
            val level = manager.getLevel() ?: throw NullPointerException("No level?")
            val random = RandomSource.create()

            val start = System.currentTimeMillis()

            mesher.submit()

            for (x in box.min.x..box.max.x) {
                for (y in box.min.y..box.max.y) {
                    for (z in box.min.z..box.max.z) {
                        val pos = BlockPos(x, y, z)

                        if (pos != origin) {
                            mesher.submit(
                                manager,
                                level,
                                pos,
                                random,
                                predicate
                            )
                        }
                    }
                }
            }

            val startB = System.currentTimeMillis() - start

            val shadowFaces = LinkedList<LightFace>()
            val lightFaces = LinkedList<LightFace>()
            mesher.finish(manager, predicate, level, shadowFaces::add, lightFaces::add)

            val finish = System.currentTimeMillis()

            val task = build(level, shadowFaces, lightFaces)

            val finishB = System.currentTimeMillis() - finish

            println("async task $startB $finishB")

            return@supplyAsync task
        }
    }
}