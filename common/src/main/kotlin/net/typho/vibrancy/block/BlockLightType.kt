package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.LightManager

interface BlockLightType<I : BlockLightInfo, B : BlockLight<I>, S : BlockLightStorage<I>> {
    fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<I>

    fun createStorage(): S

    fun render(manager: LightManager, lights: S): BlockRenderResult
}