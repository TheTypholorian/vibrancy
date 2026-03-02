package net.typho.vibrancy

import java.util.function.Consumer

data class LightRenderResult(
    var numRendered: Int? = null,
    var numAsyncTasks: Int? = null,
) {
    fun add(other: LightRenderResult) {
        numRendered = if (numRendered == null && other.numRendered == null) null else (numRendered ?: 0) + (other.numRendered ?: 0)
        numAsyncTasks = if (numAsyncTasks == null && other.numAsyncTasks == null) null else (numAsyncTasks ?: 0) + (other.numAsyncTasks ?: 0)
    }

    fun accept(out: Consumer<String>) {
        numRendered?.let { out.accept("$it rendered") }
        numAsyncTasks?.let { out.accept("$it async tasks") }
    }
}