package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.LightManager

interface BlockLightType<I : BlockLightInfo, B : BlockLight<I>> {
    fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<I>

    fun render(manager: LightManager, lights: Set<RenderingBlockLight<B>>)
}