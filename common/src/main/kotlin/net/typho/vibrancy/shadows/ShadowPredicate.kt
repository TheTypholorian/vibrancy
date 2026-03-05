package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

interface ShadowPredicate {
    fun shouldCastBlock(
        level: Level,
        pos: BlockPos
    ): Boolean

    fun shouldCastFluid(
        level: Level,
        pos: BlockPos
    ): Boolean

    fun shouldCastFace(
        face: Direction?,
        level: Level,
        pos: BlockPos
    ): Boolean

    fun isInLightRange(pos: BlockPos): Boolean

    fun isInShadowRange(pos: BlockPos): Boolean
}