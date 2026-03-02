package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.LightManager
import java.util.*
import java.util.function.Consumer

open class BasicShadowMesher : ShadowMesher {
    val shadowFaces = LinkedList<LightFace>()
    val lightFaces = LinkedList<LightFace>()

    override fun submit(
        manager: LightManager,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        random: RandomSource,
        predicate: ShadowPredicate
    ) {
        if (predicate.shouldCastBlock(state, level, pos)) {
            val shadow = predicate.isInShadowRange(pos)
            val light = predicate.isInLightRange(pos)

            if (shadow || light) {
                ShadowMesher.collectLightFaces(manager, state, level, pos, predicate) { face ->
                    if (shadow) {
                        shadowFaces.add(face)
                    }

                    if (light) {
                        lightFaces.add(face)
                    }
                }
            }
        }
    }

    override fun finish(
        manager: LightManager,
        predicate: ShadowPredicate,
        level: Level,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    ) {
        shadowFaces.forEach(shadowOut)
        lightFaces.forEach(lightOut)
    }
}