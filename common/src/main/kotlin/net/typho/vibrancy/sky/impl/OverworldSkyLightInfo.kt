package net.typho.vibrancy.sky.impl

import net.minecraft.world.level.Level
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.sky.SkyLightInfo
import org.joml.Vector3f

class OverworldSkyLightInfo(
    val sunColor: Vector3f,
    val moonColor: Vector3f
) : SkyLightInfo<OverworldSkyLightInfo, OverworldSkyLight> {
    override fun type() = OverworldSkyLightType

    override fun createSkyLight(
        manager: LightManager,
        level: Level
    ) = OverworldSkyLight(this)
}