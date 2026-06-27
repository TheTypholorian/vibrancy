package net.typho.vibrancy.util

import net.minecraft.util.profiling.ProfilerFiller

interface GlTask<V> {
    val isDone: Boolean
    val isCancelled: Boolean

    fun isDoneOrCancelled(): Boolean = isDone || isCancelled

    fun cancel()

    fun finish(profiler: ProfilerFiller? = null): V?
}