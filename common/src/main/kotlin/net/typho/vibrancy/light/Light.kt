package net.typho.vibrancy.light

import net.minecraft.client.Camera
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.gl.GlStack

interface Light {
    fun render(manager: LightManager, raytrace: Boolean, stack: GlStack)

    fun getCullingBox(): AABB?

    fun testCullingDistance(camera: Camera, chunks: Int): Boolean
}