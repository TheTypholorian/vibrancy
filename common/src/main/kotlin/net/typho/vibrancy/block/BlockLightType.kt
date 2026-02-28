package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult

interface BlockLightType<I, S : BlockLightStorage<I>> {
    fun createStorage(manager: LightManager): S

    fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<I>

    fun castInfo(info: Any?): I?

    fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: S,
        fbo: GlFramebuffer
    ): LightRenderResult
}