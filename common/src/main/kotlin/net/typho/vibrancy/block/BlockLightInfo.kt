package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.LightManager

interface BlockLightInfo<I : BlockLightInfo<I, B>, B : BlockLight<I, B>> {
    fun type(): BlockLightType<I, B, *>

    fun createBlockLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: BlockPos
    ): B?

    fun shouldCastShadow(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: BlockPos
    ): Boolean = true
}