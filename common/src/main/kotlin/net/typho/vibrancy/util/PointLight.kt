package net.typho.vibrancy.util

import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.vibrancy.shadows.ShadowPredicate

interface PointLight {
    val pos: AbstractVec3<Int>?
    val absolutePos: AbstractVec3<Float>
    val boundingBox: AbstractRect3<Int>
    val shadowBox: AbstractRect3<Int>
    val shadowPredicate: ShadowPredicate?
}