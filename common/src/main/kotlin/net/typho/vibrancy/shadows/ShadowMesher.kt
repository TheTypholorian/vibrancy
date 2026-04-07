package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.big_shot_lib.api.math.vec.NeoVec3f
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager

interface ShadowMesher {
    fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        predicate: LightFacePredicate,
        out: (face: LightFace) -> Unit
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
            out: (dir: NeoDirection?, face: LightFace) -> Unit,
            collectFluid: Boolean = true
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

                                vertex = tintColor?.let { vertex.withColor { tintColor } } ?: vertex.withColor { NeoColor.FULL_ON }
                                vertex = quad.direction?.let { dir -> vertex.withNormal { dir.inc.toFloat() } } ?: vertex.withNormal { NeoVec3f(0f, 0f, 0f) }

                                return@withVertices vertex
                            }

                            out(
                                dir,
                                LightFace(
                                    pos,
                                    state,
                                    quad,
                                    atlas
                                )
                            )
                        }
                    }
                }

                if (collectFluid) {
                    BlockUtil.INSTANCE.getFluidQuads(
                        state,
                        level.getFluidState(pos.blockPos),
                        level,
                        pos,
                        { level, from, direction, otherState -> false },
                        { quad ->
                            out(
                                quad.direction, LightFace(
                                    pos,
                                    state,
                                    quad.withVertices { index, vertex ->
                                        vertex.withPosition { vertexPos ->
                                            vertexPos.plus(
                                                (pos.x and 15.inv()).toFloat(),
                                                (pos.y and 15.inv()).toFloat(),
                                                (pos.z and 15.inv()).toFloat()
                                            )
                                        }
                                    },
                                    atlas
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}