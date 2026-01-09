package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

interface FaceCastingPredicate {
    fun shouldCast(
        face: Direction?,
        state: BlockState,
        level: Level,
        pos: BlockPos
    ): Boolean

    fun isInRange(pos: BlockPos): Boolean
}