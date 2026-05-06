package net.typho.vibrancy.sky.impl

import net.minecraft.util.profiling.ProfilerFiller
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlFramebuffer
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightType

object OverworldSkyLightType : SkyLightType<OverworldSkyLightInfo, OverworldSkyLightStorage> {
    override val infoCodec = OverworldSkyLightInfo.CODEC

    override fun createStorage(manager: LightManager) = OverworldSkyLightStorage()

    override fun castInfo(info: SkyLightInfo?): OverworldSkyLightInfo? {
        return info as? OverworldSkyLightInfo
    }

    override fun render(
        manager: LightManager,
        result: GlFramebuffer,
        temp: GlFramebuffer,
        data: RenderEventData,
        lights: OverworldSkyLightStorage,
        debugOut: (key: String, value: Int) -> Unit,
        profiler: ProfilerFiller
    ) {
        lights.render(data, manager, result, temp, debugOut, profiler)
    }
}