package net.typho.vibrancy.shadows

import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.vibrancy.LightManager

class BasicMesher(
    @JvmField
    val blocks: Iterable<AbstractVec3<Int>>
) : ShadowMesher {
    override fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        predicate: LightFacePredicate,
        out: (face: LightFace) -> Unit
    ) {
        for (pos in blocks) {
            val state = level.getBlockState(pos.blockPos)

            ShadowMesher.collectLightFaces(
                manager,
                state,
                level,
                pos,
                atlas,
                { predicate.shouldCastFace(it, level, pos, state) },
                { dir, face -> out(face) }
            )
        }
    }
}