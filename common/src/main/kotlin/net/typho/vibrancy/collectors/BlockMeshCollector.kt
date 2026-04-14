package net.typho.vibrancy.collectors

import net.minecraft.client.Minecraft
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoVertexData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.shadows.LightFace

interface BlockMeshCollector {
    fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        vararg consumers: Consumer
    )

    interface Predicate {
        fun shouldCastBlock(
            level: Level,
            pos: IVec3<Int>,
            state: BlockState?
        ): Boolean

        fun shouldCastFace(
            face: NeoDirection?,
            level: Level,
            pos: IVec3<Int>,
            state: BlockState?
        ): Boolean
    }

    interface Consumer {
        val predicate: Predicate

        fun collect(faces: Iterable<LightFace>)
    }

    companion object {
        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            state: BlockState,
            level: Level,
            pos: IVec3<Int>,
            atlas: NeoAtlas,
            collectFluid: Boolean,
            vararg consumers: Consumer
        ) {
            if (!state.isAir) {
                BlockUtil.INSTANCE.getBlockQuads(state, level, pos) { dir, quads ->
                    val faces = quads.map { quad ->
                        val tintColor = if (quad.tintIndex != null) NeoColor.RGB(Minecraft.getInstance().blockColors.getColor(state, level, pos.blockPos, quad.tintIndex!!)) else null

                        LightFace(
                            pos,
                            state,
                            quad.withVertices { index, vertex ->
                                NeoVertexData(
                                    vertex,
                                    pos = vertex.pos + pos.toFloat(),
                                    color = tintColor,
                                    normal = quad.direction?.toFloat()
                                )
                            },
                            atlas
                        )
                    }

                    for (consumer in consumers) {
                        if (consumer.predicate.shouldCastFace(dir, level, pos, state)) {
                            consumer.collect(faces)
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
                            val face = listOf(LightFace(
                                pos,
                                state,
                                quad.withVertices { index, vertex ->
                                    NeoVertexData(
                                        vertex,
                                        pos = vertex.pos.plus(
                                            (pos.x and 15.inv()).toFloat(),
                                            (pos.y and 15.inv()).toFloat(),
                                            (pos.z and 15.inv()).toFloat()
                                        )
                                    )
                                },
                                atlas
                            ))

                            for (consumer in consumers) {
                                consumer.collect(face)
                            }
                        }
                    )
                }
            }
        }
    }
}