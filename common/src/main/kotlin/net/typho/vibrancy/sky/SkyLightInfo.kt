package net.typho.vibrancy.sky

import net.minecraft.world.level.Level
import net.typho.vibrancy.LightManager

interface SkyLightInfo<I : SkyLightInfo<I, L>, L : SkyLight<I, L>> {
    fun type(): SkyLightType<I, L, *>

    fun createSkyLight(
        manager: LightManager,
        level: Level
    ): L?
}