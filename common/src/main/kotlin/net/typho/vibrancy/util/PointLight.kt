package net.typho.vibrancy.util

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.typho.vibrancy.shadows.ShadowPredicate
import org.joml.Vector3f

interface PointLight {
    val blockPos: BlockPos?
    val absolutePos: Vector3f
    val boundingBox: AABB
    val shadowBox: BlockBox
    val shadowPredicate: ShadowPredicate?
}