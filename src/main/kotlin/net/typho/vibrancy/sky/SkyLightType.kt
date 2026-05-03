package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.minecraft.util.profiling.ProfilerFiller
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.state.NeoGlStateManager
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.vibrancy.LightManager

interface SkyLightType<I : SkyLightInfo, S : SkyLightStorage<I>> {
    val infoCodec: MapCodec<I>

    fun createStorage(manager: LightManager): S

    fun castInfo(info: SkyLightInfo?): I?

    fun render(
        manager: LightManager,
        result: GlFramebuffer,
        temp: GlFramebuffer,
        data: RenderEventData,
        lights: S,
        debugOut: (key: String, value: Int) -> Unit,
        profiler: ProfilerFiller
    )
}