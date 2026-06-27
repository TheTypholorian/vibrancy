package net.typho.vibrancy.util

import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.vec.IVec3

interface PointLight {
    val pos: IVec3<Int>?
    val absolutePos: IVec3<Float>
    val boundingBox: AbstractRect3<Int>
}