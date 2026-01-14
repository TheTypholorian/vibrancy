package net.typho.vibrancy.block

import net.minecraft.world.phys.AABB
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.util.PointLight

interface BlockLight<I : BlockLightInfo<I, B>, B : BlockLight<I, B>> : PointLight {
    fun getBoundingBox(): AABB

    fun rebuildShadows(manager: LightManager)

    fun getType(): BlockLightType<I, B, *>

    fun shouldRender(manager: LightManager): Boolean

    fun shouldRaytrace(manager: LightManager): Boolean

    fun free(manager: LightManager)
}