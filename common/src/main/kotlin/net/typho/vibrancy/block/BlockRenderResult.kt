package net.typho.vibrancy.block

data class BlockRenderResult(
    var numRendered: Int = 0,
    var numRaytraced: Int = 0,
    var numShadows: Int = 0,
    var numAsyncTasks: Int = 0,
    var numEntityShadowCalls: Int = 0,
) {
    fun add(other: BlockRenderResult) {
        numRendered += other.numRendered
        numRaytraced += other.numRaytraced
        numShadows += other.numShadows
        numAsyncTasks += other.numAsyncTasks
        numEntityShadowCalls += other.numEntityShadowCalls
    }
}