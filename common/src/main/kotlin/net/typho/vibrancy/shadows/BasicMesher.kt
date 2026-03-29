package net.typho.vibrancy.shadows

import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.vibrancy.LightManager
import java.util.function.Consumer

class BasicMesher(
    @JvmField
    val blocks: Iterable<AbstractVec3<Int>>
) : ShadowMesher {
    override fun submit(
        manager: LightManager,
        level: Level,
        predicate: ShadowPredicate,
        atlas: NeoAtlas,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    ) {
        for (pos in blocks) {
            val shadow = predicate.isInShadowRange(pos)
            val light = predicate.isInLightRange(pos)

            if (shadow || light) {
                val state = level.getBlockState(pos.blockPos)

                ShadowMesher.collectLightFaces(
                    manager,
                    state,
                    level,
                    pos,
                    atlas,
                    { predicate.shouldCastFace(it, level, pos, state) }
                ) { dir, face ->
                    if (shadow) {
                        shadowOut.accept(face)
                    }

                    if (light) {
                        lightOut.accept(face)
                    }
                }
            }
        }
    }
}