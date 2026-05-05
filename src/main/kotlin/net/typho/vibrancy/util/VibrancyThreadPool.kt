package net.typho.vibrancy.util

import net.minecraft.world.level.ChunkPos
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.VibrancyConfig
import java.util.concurrent.CompletableFuture
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object VibrancyThreadPool : ThreadPoolExecutor(
    VibrancyConfig.asyncThreads,
    VibrancyConfig.asyncThreads,
    10L,
    TimeUnit.MINUTES,
    PriorityBlockingQueue(11, Comparator.comparingDouble { a -> if (a is SortedAsyncTask) a.sortingOrder else 0.0 })
) {
    @JvmStatic
    fun <T> submit(sort: Double, task: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        submit(object : SortedAsyncTask {
            override val sortingOrder: Double = sort

            override fun run() {
                try {
                    future.complete(task())
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        })
        return future
    }

    @JvmStatic
    fun <T> submit(data: RenderEventData, chunk: ChunkPos, manager: LightManager, task: () -> T): CompletableFuture<T> {
        return submit(manager.getSortingOrder(data, chunk).toDouble(), task)
    }

    @JvmStatic
    fun <T> submit(data: RenderEventData, pos: IVec3<Int>, manager: LightManager, task: () -> T): CompletableFuture<T> {
        return submit(manager.getSortingOrder(data, pos).toDouble(), task)
    }
}