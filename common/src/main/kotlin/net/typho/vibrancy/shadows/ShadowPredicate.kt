package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState

interface ShadowPredicate {
    fun shouldCastBlock(
        block: BlockState,
        level: Level,
        pos: BlockPos
    ): Boolean

    fun shouldCastFluid(
        fluid: FluidState,
        level: Level,
        pos: BlockPos
    ): Boolean

    fun shouldCastFace(
        face: Direction?,
        state: BlockState,
        level: Level,
        pos: BlockPos
    ): Boolean

    fun isInLightRange(pos: BlockPos): Boolean

    fun isInShadowRange(pos: BlockPos): Boolean
}