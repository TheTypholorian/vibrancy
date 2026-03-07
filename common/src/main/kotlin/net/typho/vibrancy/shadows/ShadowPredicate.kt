package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

interface ShadowPredicate {
    fun shouldCastBlock(
        level: Level,
        pos: BlockPos,
        state: BlockState = level.getBlockState(pos)
    ): Boolean

    fun shouldCastFace(
        face: Direction?,
        level: Level,
        pos: BlockPos,
        state: BlockState = level.getBlockState(pos)
    ): Boolean

    fun isInLightRange(pos: BlockPos): Boolean

    fun isInShadowRange(pos: BlockPos): Boolean
}