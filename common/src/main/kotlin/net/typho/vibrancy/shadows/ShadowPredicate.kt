package net.typho.vibrancy.shadows

import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos

interface ShadowPredicate {
    fun shouldCastBlock(
        level: Level,
        pos: AbstractVec3<Int>,
        state: BlockState = level.getBlockState(pos.blockPos)
    ): Boolean

    fun shouldCastFace(
        face: NeoDirection?,
        level: Level,
        pos: AbstractVec3<Int>,
        state: BlockState = level.getBlockState(pos.blockPos)
    ): Boolean

    fun isInLightRange(pos: AbstractVec3<Int>): Boolean

    fun isInShadowRange(pos: AbstractVec3<Int>): Boolean
}