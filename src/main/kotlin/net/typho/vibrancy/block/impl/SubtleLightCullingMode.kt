package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.BlockUtil

enum class SubtleLightCullingMode(
    @JvmField
    val test: (level: Level, neighborPos: BlockPos, source: BlockState, neighbor: BlockState) -> Boolean
) {
    SAME_NEIGHBOR({ level, neighborPos, source, neighbor -> source.block == neighbor.block }),
    SOLID_NEIGHBOR({ level, neighborPos, source, neighbor -> source.block == neighbor.block || BlockUtil.INSTANCE.isSolidRender(neighbor, neighborPos, level) }),
    NON_AIR_NEIGHBOR({ level, neighborPos, source, neighbor -> !neighbor.isAir })
}