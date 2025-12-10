package net.typho.vibrancy.api

import net.minecraft.world.phys.AABB

interface Light {
    fun render(manager: LightManager, raytrace: Boolean)

    fun getCullingBox(): AABB?

    fun testCullingDistance(chunks: Int): Boolean
}