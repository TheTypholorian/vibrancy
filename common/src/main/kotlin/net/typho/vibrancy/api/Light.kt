package net.typho.vibrancy.api

import net.minecraft.world.phys.AABB
import net.typho.vibrancy.LightManager

interface Light {
    fun render(manager: LightManager)

    fun getCullingBox(): AABB?
}