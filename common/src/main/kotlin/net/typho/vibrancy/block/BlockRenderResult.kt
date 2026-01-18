package net.typho.vibrancy.block

data class BlockRenderResult(
    var numRendered: Int? = null,
    var numRaytraced: Int? = null,
    var numShadows: Int? = null,
    var numAsyncTasks: Int? = null,
    var numEntityShadowCalls: Int? = null,
) {
    fun add(other: BlockRenderResult) {
        numRendered = if (numRendered == null && other.numRendered == null) null else (numRendered ?: 0) + (other.numRendered ?: 0)
        numRaytraced = if (numRaytraced == null && other.numRaytraced == null) null else (numRaytraced ?: 0) + (other.numRaytraced ?: 0)
        numShadows = if (numShadows == null && other.numShadows == null) null else (numShadows ?: 0) + (other.numShadows ?: 0)
        numAsyncTasks = if (numAsyncTasks == null && other.numAsyncTasks == null) null else (numAsyncTasks ?: 0) + (other.numAsyncTasks ?: 0)
        numEntityShadowCalls = if (numEntityShadowCalls == null && other.numEntityShadowCalls == null) null else (numEntityShadowCalls ?: 0) + (other.numEntityShadowCalls ?: 0)
    }
}