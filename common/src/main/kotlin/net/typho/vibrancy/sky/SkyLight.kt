package net.typho.vibrancy.sky

import net.typho.vibrancy.LightManager

interface SkyLight<I : SkyLightInfo<I, L>, L : SkyLight<I, L>> {
    fun rebuildShadows(manager: LightManager)

    fun getType(): SkyLightType<I, L, *>

    fun free(manager: LightManager)
}