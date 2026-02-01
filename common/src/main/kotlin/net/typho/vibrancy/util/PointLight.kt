package net.typho.vibrancy.util

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.typho.vibrancy.shadows.ShadowPredicate
import org.joml.Vector3f

interface PointLight {
    fun getBlockPos(): BlockPos?

    fun getAbsolutePos(): Vector3f

    fun getShadowBox(fullQuality: Boolean): BlockBox?

    fun getShadowPredicate(fullQuality: Boolean): ShadowPredicate?
}