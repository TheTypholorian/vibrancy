package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult

interface SkyLightType<I, S : SkyLightStorage<I>> {
    fun createStorage(manager: LightManager): S

    fun infoCodec(): MapCodec<I>

    fun castInfo(info: Any?): I?

    fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: S,
        fbo: GlFramebuffer
    ): LightRenderResult
}