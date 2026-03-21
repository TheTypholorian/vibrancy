package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.util.quads.NeoAtlas
import net.typho.vibrancy.LightManager
import java.util.function.Consumer

class BasicMesher(
    @JvmField
    val blocks: Iterable<BlockPos>
) : ShadowMesher {
    override fun submit(
        manager: LightManager,
        level: Level,
        predicate: ShadowPredicate,
        atlas: NeoAtlas,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>,
        splitLargeLightFaces: Boolean
    ) {
        for (pos in blocks) {
            val shadow = predicate.isInShadowRange(pos)
            val light = predicate.isInLightRange(pos)

            if (shadow || light) {
                val state = level.getBlockState(pos)

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
                        if (splitLargeLightFaces) {
                            face.split(16).forEach(lightOut::accept)
                        } else {
                            lightOut.accept(face)
                        }
                    }
                }
            }
        }
    }
}