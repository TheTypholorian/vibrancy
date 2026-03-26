package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager

interface SkyLightType<I : SkyLightInfo, S : SkyLightStorage<I>> {
    val infoCodec: MapCodec<I>

    fun createStorage(manager: LightManager): S

    fun castInfo(info: SkyLightInfo?): I?

    fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: S,
        debugOut: (key: String, value: Int) -> Unit
    )
}