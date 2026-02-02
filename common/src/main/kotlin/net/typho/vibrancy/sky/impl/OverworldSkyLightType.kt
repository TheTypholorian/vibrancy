package net.typho.vibrancy.sky.impl

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.Level
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.sky.SkyLightType
import net.typho.vibrancy.sky.SkyRenderResult

object OverworldSkyLightType : SkyLightType<OverworldSkyLightInfo, OverworldSkyLight, OverworldSkyLightStorage> {
    override fun infoCodec(level: Level): MapCodec<OverworldSkyLightInfo> {
        TODO("Not yet implemented")
    }

    override fun createStorage() = OverworldSkyLightStorage()

    override fun render(
        manager: LightManager,
        lights: OverworldSkyLightStorage
    ): SkyRenderResult {
        TODO("Not yet implemented")
    }
}