package net.typho.vibrancy

import java.util.function.Consumer

data class LightRenderResult(
    var numRendered: Int? = null,
    var numRaytraced: Int? = null,
    var numForeground: Int? = null,
    var numShadows: Int? = null,
    var numAsyncTasks: Int? = null,
) {
    fun add(other: LightRenderResult) {
        numRendered = if (numRendered == null && other.numRendered == null) null else (numRendered ?: 0) + (other.numRendered ?: 0)
        numRaytraced = if (numRaytraced == null && other.numRaytraced == null) null else (numRaytraced ?: 0) + (other.numRaytraced ?: 0)
        numForeground = if (numForeground == null && other.numForeground == null) null else (numForeground ?: 0) + (other.numForeground ?: 0)
        numShadows = if (numShadows == null && other.numShadows == null) null else (numShadows ?: 0) + (other.numShadows ?: 0)
        numAsyncTasks = if (numAsyncTasks == null && other.numAsyncTasks == null) null else (numAsyncTasks ?: 0) + (other.numAsyncTasks ?: 0)
    }

    fun accept(out: Consumer<String>) {
        numRendered?.let { out.accept("$it rendered") }
        numRaytraced?.let { out.accept("$it raytraced") }
        numForeground?.let { out.accept("$it foreground") }
        numShadows?.let { out.accept("$it shadows") }
        numAsyncTasks?.let { out.accept("$it async tasks") }
    }
}