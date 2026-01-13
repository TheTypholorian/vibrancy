package net.typho.vibrancy.block

import net.minecraft.world.phys.AABB
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.util.PointLight

interface BlockLight<I : BlockLightInfo> : PointLight {
    fun getBoundingBox(): AABB

    fun rebuildShadows(manager: LightManager)

    fun getType(): BlockLightType<I, *>

    fun shouldRender(manager: LightManager): Boolean

    fun shouldRaytrace(manager: LightManager): Boolean

    fun numShadows(): Int

    fun numAsyncTasksActive(): Int

    fun free(manager: LightManager)
}