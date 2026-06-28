package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

enum class SubtleLightCullingMode(
    @JvmField
    val test: (level: Level, neighborPos: BlockPos, source: BlockState, neighbor: BlockState) -> Boolean
) {
    SAME_NEIGHBOR({ level, neighborPos, source, neighbor -> source.block == neighbor.block }),
    SOLID_NEIGHBOR({ level, neighborPos, source, neighbor -> source.block == neighbor.block || neighbor.isSolidRender }),
    NON_AIR_NEIGHBOR({ level, neighborPos, source, neighbor -> !neighbor.isAir })
}