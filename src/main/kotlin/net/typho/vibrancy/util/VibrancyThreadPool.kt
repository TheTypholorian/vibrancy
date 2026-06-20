package net.typho.vibrancy.util

import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
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
    fun <T> submit(sort: Double, task: (isCancelled: () -> Boolean) -> Pair<AutoCloseable, () -> T>): GlTask<T> {
        val future = CompletableFuture<Pair<AutoCloseable, () -> T>>()
        var cancelled = false
        var result: T? = null
        submit(object : SortedAsyncTask {
            override val sortingOrder: Double = sort

            override fun run() {
                try {
                    val r = task { cancelled }

                    if (cancelled) {
                        r.first.close()
                    }

                    future.complete(r)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        })
        return object : GlTask<T> {
            override val isDone: Boolean
                get() = future.isDone
            override val isCancelled: Boolean
                get() = cancelled

            override fun cancel() {
                cancelled = true
            }

            override fun finish(): T? {
                result?.let { return it }

                if (!isCancelled && isDone) {
                    val r = future.get()
                    val v = r.second()
                    result = v
                    r.first.close()
                    return v
                } else {
                    return null
                }
            }
        }
    }

    @JvmStatic
    fun <T> submit(data: RenderEventData, chunk: ChunkPos, manager: LightManager, task: (isCancelled: () -> Boolean) -> Pair<AutoCloseable, () -> T>): GlTask<T> {
        return submit(manager.getSortingOrder(data, chunk).toDouble(), task)
    }

    @JvmStatic
    fun <T> submit(data: RenderEventData, chunk: SectionPos, manager: LightManager, task: (isCancelled: () -> Boolean) -> Pair<AutoCloseable, () -> T>): GlTask<T> {
        return submit(manager.getSortingOrder(data, chunk).toDouble(), task)
    }

    @JvmStatic
    fun <T> submit(data: RenderEventData, pos: IVec3<Int>, manager: LightManager, task: (isCancelled: () -> Boolean) -> Pair<AutoCloseable, () -> T>): GlTask<T> {
        return submit(manager.getSortingOrder(data, pos).toDouble(), task)
    }

    @JvmStatic
    fun <T> submitClean(sort: Double, task: (isCancelled: () -> Boolean) -> T): GlTask<T> {
        return submit(sort) { isCancelled ->
            val result = task(isCancelled)
            AutoCloseable { } to { result }
        }
    }

    @JvmStatic
    fun <T> submitClean(data: RenderEventData, chunk: ChunkPos, manager: LightManager, task: (isCancelled: () -> Boolean) -> T): GlTask<T> {
        return submitClean(manager.getSortingOrder(data, chunk).toDouble(), task)
    }

    @JvmStatic
    fun <T> submitClean(data: RenderEventData, chunk: SectionPos, manager: LightManager, task: (isCancelled: () -> Boolean) -> T): GlTask<T> {
        return submitClean(manager.getSortingOrder(data, chunk).toDouble(), task)
    }

    @JvmStatic
    fun <T> submitClean(data: RenderEventData, pos: IVec3<Int>, manager: LightManager, task: (isCancelled: () -> Boolean) -> T): GlTask<T> {
        return submitClean(manager.getSortingOrder(data, pos).toDouble(), task)
    }
}