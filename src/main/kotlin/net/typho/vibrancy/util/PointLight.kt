package net.typho.vibrancy.util

import net.typho.big_shot_lib.api.math.IRect3
import net.typho.big_shot_lib.api.math.IVec3

interface PointLight {
    val pos: IVec3<Int>?
    val absolutePos: IVec3<Float>
    val boundingBox: IRect3<Float>
}