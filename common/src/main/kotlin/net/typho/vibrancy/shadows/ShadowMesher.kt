package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.opengl.util.MeshUtil
import net.typho.vibrancy.LightManager
import java.util.function.Consumer

interface ShadowMesher {
    fun submit(
        manager: LightManager,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        random: RandomSource,
        predicate: ShadowPredicate
    )

    fun finish(
        manager: LightManager,
        predicate: ShadowPredicate,
        level: Level,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    )

    companion object {
        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            state: BlockState,
            level: Level,
            pos: BlockPos,
            predicate: ShadowPredicate,
            out: Consumer<LightFace>
        ) {
            if (state.isAir) {
                return
            }

            MeshUtil.INSTANCE.getBlockQuads(state, level, pos) { dir, quads ->
                if (predicate.shouldCastFace(dir, state, level, pos)) {
                    quads.forEach { out.accept(LightFace(pos, it.offset(Vec3.atLowerCornerOf(pos).toVector3f()), 1, 1)) }
                }
            }
        }
    }
}