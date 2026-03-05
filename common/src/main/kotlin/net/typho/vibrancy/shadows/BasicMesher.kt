package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
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
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    ) {
        for (pos in blocks) {
            val shadow = predicate.isInShadowRange(pos)
            val light = predicate.isInLightRange(pos)

            if (shadow || light) {
                ShadowMesher.collectLightFaces(
                    manager,
                    level.getBlockState(pos),
                    level,
                    pos,
                    { predicate.shouldCastFace(it, level, pos) }
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