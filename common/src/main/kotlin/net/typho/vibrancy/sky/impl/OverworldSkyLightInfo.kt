package net.typho.vibrancy.sky.impl

import net.minecraft.world.level.Level
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightType

class OverworldSkyLightInfo : SkyLightInfo<OverworldSkyLightInfo, OverworldSkyLight> {
    override fun type(): SkyLightType<OverworldSkyLightInfo, OverworldSkyLight, *> {
        TODO("Not yet implemented")
    }

    override fun createSkyLight(
        manager: LightManager,
        level: Level
    ): OverworldSkyLight? {
        TODO("Not yet implemented")
    }
}