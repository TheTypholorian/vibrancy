package net.typho.vibrancy.collectors

import net.minecraft.client.Minecraft
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
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
        fun isBlockTransparent(
            level: Level,
            pos: IVec3<Int>,
            state: BlockState
        ): Boolean = !BlockUtil.INSTANCE.isSolidRender(state, pos, level)

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

        fun collect(faces: Iterable<LightFace>, origin: FaceOrigin)
    }

    interface FaceOrigin {
        data class Block(
            @JvmField
            val block: BlockState,
            @JvmField
            val pos: IVec3<Int>
        ) : FaceOrigin

        data class Fluid(
            @JvmField
            val fluid: FluidState,
            @JvmField
            val pos: IVec3<Int>
        ) : FaceOrigin
    }

    companion object {
        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            state: BlockState,
            level: Level,
            pos: IVec3<Int>,
            offset: IVec3<Int>,
            atlas: NeoAtlas,
            collectFluid: Boolean,
            vararg consumers: Consumer
        ) {
            if (!state.isAir) {
                val consumers = consumers.filter { it.predicate.shouldCastBlock(level, pos, state) }

                BlockUtil.INSTANCE.getBlockQuads(state, level, pos) { dir, quads ->
                    val faces = quads.map { quad ->
                        val tintColor = if (quad.tintIndex != null) NeoColor.RGB(Minecraft.getInstance().blockColors.getColor(state, level, pos.blockPos, quad.tintIndex!!)) else null

                        LightFace(
                            pos,
                            state,
                            quad.withVertices { index, vertex ->
                                NeoVertexData(
                                    vertex,
                                    pos = vertex.pos + offset.toFloat(),
                                    color = tintColor,
                                    normal = quad.direction?.toFloat()
                                )
                            },
                            atlas
                        )
                    }

                    val origin = FaceOrigin.Block(state, pos)

                    for (consumer in consumers) {
                        if (consumer.predicate.shouldCastFace(dir, level, pos, state)) {
                            consumer.collect(faces, origin)
                        }
                    }
                }

                if (collectFluid) {
                    val fluid = level.getFluidState(pos.blockPos)
                    val origin = FaceOrigin.Fluid(fluid, pos)

                    BlockUtil.INSTANCE.getFluidQuads(
                        state,
                        fluid,
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
                                        pos = vertex.pos.minus(
                                            (pos.x and 15).toFloat(),
                                            (pos.y and 15).toFloat(),
                                            (pos.z and 15).toFloat()
                                        ) + offset.toFloat()
                                    )
                                },
                                atlas
                            ))

                            for (consumer in consumers) {
                                consumer.collect(face, origin)
                            }
                        }
                    )
                }
            }
        }
    }
}