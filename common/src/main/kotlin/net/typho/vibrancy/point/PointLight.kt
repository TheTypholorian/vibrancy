package net.typho.vibrancy.point

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.typho.vibrancy.shadows.ShadowPredicate
import org.joml.Vector3f

interface PointLight {
    fun getBlockPos(): BlockPos?

    fun getAbsolutePos(): Vector3f

    fun getShadowBox(): BlockBox

    fun getShadowPredicate(): ShadowPredicate
}