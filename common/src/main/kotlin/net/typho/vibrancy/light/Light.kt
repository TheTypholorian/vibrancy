package net.typho.vibrancy.light

import net.minecraft.client.Camera
import net.minecraft.world.phys.AABB

interface Light {
    fun render(manager: LightManager, raytrace: Boolean)

    fun getCullingBox(): AABB?

    fun testCullingDistance(camera: Camera, chunks: Int): Boolean
}