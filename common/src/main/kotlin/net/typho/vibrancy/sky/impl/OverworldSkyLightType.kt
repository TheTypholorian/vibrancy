package net.typho.vibrancy.sky.impl

import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.sky.SkyLightType

object OverworldSkyLightType : SkyLightType<OverworldSkyLightInfo, OverworldSkyLightStorage> {
    override val infoCodec = OverworldSkyLightInfo.CODEC

    override fun createStorage(manager: LightManager) = OverworldSkyLightStorage()

    override fun castInfo(info: Any?): OverworldSkyLightInfo? {
        return info as? OverworldSkyLightInfo
    }

    override fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: OverworldSkyLightStorage,
        fbo: GlFramebuffer
    ): LightRenderResult {
        TODO("Not yet implemented")
    }
}