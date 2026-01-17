package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.LightManager
import java.util.*
import java.util.function.Consumer

open class BasicShadowMesher : ShadowMesher {
    val shadows = LinkedList<LightFace>()

    override fun submit(
        manager: LightManager,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        random: RandomSource,
        predicate: ShadowPredicate
    ) {
        if (predicate.isInRange(pos)) {
            ShadowMesher.collectLightFaces(manager, state, level, pos, predicate, shadows::add)
        }
    }

    override fun finish(
        manager: LightManager,
        predicate: ShadowPredicate,
        level: Level,
        out: Consumer<LightFace>
    ) {
        for (face in shadows) {
            out.accept(face)
        }
    }
}