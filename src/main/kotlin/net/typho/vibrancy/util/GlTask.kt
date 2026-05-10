package net.typho.vibrancy.util

interface GlTask<V> {
    val isDone: Boolean
    val isCancelled: Boolean

    fun isDoneOrCancelled(): Boolean = isDone || isCancelled

    fun cancel()

    fun finish(): V?
}