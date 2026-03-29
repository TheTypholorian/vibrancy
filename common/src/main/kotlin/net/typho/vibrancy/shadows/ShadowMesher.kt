package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import java.util.function.BiConsumer
import java.util.function.Consumer

interface ShadowMesher {
    fun submit(
        manager: LightManager,
        level: Level,
        predicate: ShadowPredicate,
        atlas: NeoAtlas,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    )

    companion object {
        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            state: BlockState,
            level: Level,
            pos: AbstractVec3<Int>,
            atlas: NeoAtlas,
            predicate: (face: NeoDirection?) -> Boolean,
            out: BiConsumer<NeoDirection?, LightFace>
        ) {
            if (!state.isAir) {
                BlockUtil.INSTANCE.getBlockQuads(state, level, pos) { dir, quads ->
                    if (predicate(dir)) {
                        quads.forEach { quad ->
                            var quad = quad
                            val tintColor = if (quad.tintIndex != null) NeoColor.RGB(Minecraft.getInstance().blockColors.getColor(state, level, pos.blockPos, quad.tintIndex!!)) else null

                            quad = quad.withVertices { index, vertex ->
                                var vertex = vertex.withPosition { v ->
                                    v + pos.toFloat()
                                }

                                tintColor?.let { vertex = vertex.withColor { tintColor } }
                                quad.direction?.let { dir -> vertex = vertex.withNormal { dir.inc.toFloat() } }

                                return@withVertices vertex
                            }

                            out.accept(
                                dir,
                                LightFace(
                                    pos,
                                    quad,
                                    atlas
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}