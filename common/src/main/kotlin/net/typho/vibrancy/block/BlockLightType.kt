package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.LightManager

interface BlockLightType<I : BlockLightInfo, B : BlockLight<I>> {
    fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<I>

    fun render(manager: LightManager, lights: Set<RenderingBlockLight<B>>): RenderResult

    data class RenderResult(
        var numRendered: Int = 0,
        var numRaytraced: Int = 0,
        var numShadows: Int = 0,
        var numAsyncTasks: Int = 0,
    ) {
        fun add(other: RenderResult) {
            numRendered += other.numRendered
            numRaytraced += other.numRaytraced
            numShadows += other.numShadows
            numAsyncTasks += other.numAsyncTasks
        }
    }
}