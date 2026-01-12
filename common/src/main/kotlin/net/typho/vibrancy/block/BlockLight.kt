package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import org.lwjgl.system.NativeResource

interface BlockLight<I : BlockLightInfo> : NativeResource {
    fun getBlockPos(): BlockPos

    fun getAbsolutePos(): Vector3f

    fun getCullingBox(): AABB

    fun getShadowBox(): AABB

    fun getType(): BlockLightType<I, *>

    fun shouldRaytrace(): Boolean

    fun numShadows(): Int

    fun numAsyncTasksActive(): Int

    fun numEntities(): Int

    fun numBlockEntities(): Int
}