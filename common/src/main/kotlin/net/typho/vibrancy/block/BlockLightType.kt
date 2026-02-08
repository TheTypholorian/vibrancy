package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.IFramebuffer
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult

interface BlockLightType<I : BlockLightInfo<I, B>, B : BlockLight<I, B>, S : BlockLightStorage<I>> {
    fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<I>

    fun createStorage(manager: LightManager): S

    fun render(manager: LightManager, lights: S, fbo: IFramebuffer): LightRenderResult
}