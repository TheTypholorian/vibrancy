package net.typho.vibrancy.block.impl

import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.collectors.BlockMeshCollector

object SubtleLightMeshCollectorPredicate : BlockMeshCollector.Predicate {
    override fun shouldCastBlock(
        level: Level,
        pos: IVec3<Int>,
        state: BlockState?
    ): Boolean {
        return true
    }

    override fun shouldCastFace(
        face: NeoDirection?,
        level: Level,
        pos: IVec3<Int>,
        state: BlockState?
    ): Boolean {
        if (face == null) {
            return true
        }

        return BlockUtil.INSTANCE.shouldRenderFace(level, pos, face, state ?: level.getBlockState(pos.blockPos))
    }
}