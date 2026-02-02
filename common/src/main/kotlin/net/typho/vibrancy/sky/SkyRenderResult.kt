package net.typho.vibrancy.sky

data class SkyRenderResult(
    var numRendered: Int = 0,
    var numRaytraced: Int = 0,
    var numShadows: Int = 0,
    var numAsyncTasks: Int = 0,
) {
    fun add(other: SkyRenderResult) {
        numRendered += other.numRendered
        numRaytraced += other.numRaytraced
        numShadows += other.numShadows
        numAsyncTasks += other.numAsyncTasks
    }
}