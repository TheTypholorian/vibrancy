package net.typho.vibrancy.api

import net.minecraft.world.phys.AABB

interface Light {
    fun render(manager: LightManager)

    fun getCullingBox(): AABB?
}