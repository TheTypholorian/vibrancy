package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.LightManager

interface SkyLightType<I : SkyLightInfo<I, L>, L : SkyLight<I, L>> {
    fun infoCodec(level: ResourceIdentifier): MapCodec<I>

    fun create(): L

    fun render(manager: LightManager, light: L)
}