package net.typho.vibrancy.sky.impl

import net.typho.vibrancy.LightManager
import net.typho.vibrancy.sky.SkyLight
import net.typho.vibrancy.sky.SkyLightType

class OverworldSkyLight : SkyLight<OverworldSkyLightInfo, OverworldSkyLight> {
    override fun rebuildShadows(manager: LightManager) {
        TODO("Not yet implemented")
    }

    override fun getType(): SkyLightType<OverworldSkyLightInfo, OverworldSkyLight, *> {
        TODO("Not yet implemented")
    }

    override fun shouldRender(manager: LightManager): Boolean {
        TODO("Not yet implemented")
    }

    override fun free(manager: LightManager) {
        TODO("Not yet implemented")
    }
}