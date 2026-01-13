package net.typho.vibrancy.block

import net.minecraft.world.phys.AABB
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.util.PointLight
import org.lwjgl.system.NativeResource

interface BlockLight<I : BlockLightInfo> : NativeResource, PointLight {
    fun getBoundingBox(): AABB

    fun rebuildShadows(manager: LightManager)

    fun getType(): BlockLightType<I, *>

    fun shouldRaytrace(): Boolean

    fun numShadows(): Int

    fun numAsyncTasksActive(): Int
}