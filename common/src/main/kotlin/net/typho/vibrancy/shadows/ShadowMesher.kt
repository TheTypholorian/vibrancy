package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.typho.vibrancy.shadows.LightFace.Companion.toLightFace
import org.joml.Vector2f
import org.joml.Vector3f
import java.util.function.Consumer

interface ShadowMesher {
    fun submit(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        predicate: FaceCastingPredicate
    )

    fun finish(out: Consumer<LightFace>)

    companion object {
        @JvmStatic
        fun collectBakedQuads(
            state: BlockState,
            level: Level,
            pos: BlockPos,
            predicate: FaceCastingPredicate,
            out: Consumer<BakedQuad>
        ) {
            val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
            val random = RandomSource.create()

            for (dir in Direction.entries) {
                if (predicate.shouldCast(dir, state, level, pos)) {
                    for (quad in model.getQuads(state, dir, random)) {
                        out.accept(quad)
                    }
                }
            }

            for (quad in model.getQuads(state, null, random)) {
                if (predicate.shouldCast(null, state, level, pos)) {
                    out.accept(quad)
                }
            }
        }

        @JvmStatic
        fun collectLightFaces(
            state: BlockState,
            level: Level,
            pos: BlockPos,
            predicate: FaceCastingPredicate,
            out: Consumer<LightFace>
        ) {
            val offset = state.getOffset(level, pos)
            collectBakedQuads(state, level, pos, predicate) { quad ->
                out.accept(
                    quad.toLightFace(
                        offset.x.toFloat(),
                        offset.y.toFloat(),
                        offset.z.toFloat(),
                        pos
                    )
                )
            }
        }

        @JvmStatic
        fun Direction.createFace(
            pos: BlockPos,
            sprite: TextureAtlasSprite,
            width: Float = 1f,
            height: Float = 1f
        ): LightFace {
            val origin = Vec3.atLowerCornerOf(pos).toVector3f()
            val vertices: Array<Vector3f> = when (this) {
                Direction.NORTH -> arrayOf(
                    Vector3f(origin),
                    Vector3f(origin).add(width, 0f, 0f),
                    Vector3f(origin).add(width, height, 0f),
                    Vector3f(origin).add(0f, height, 0f)
                )
                Direction.SOUTH -> arrayOf(
                    Vector3f(origin).add(width, 0f, 1f),
                    Vector3f(origin).add(0f, 0f, 1f),
                    Vector3f(origin).add(0f, height, 1f),
                    Vector3f(origin).add(width, height, 1f)
                )
                Direction.WEST -> arrayOf(
                    Vector3f(origin).add(0f, 0f, width),
                    Vector3f(origin),
                    Vector3f(origin).add(0f, height, 0f),
                    Vector3f(origin).add(0f, height, width)
                )
                Direction.EAST -> arrayOf(
                    Vector3f(origin).add(1f, 0f, 0f),
                    Vector3f(origin).add(1f, 0f, width),
                    Vector3f(origin).add(1f, height, width),
                    Vector3f(origin).add(1f, height, 0f)
                )
                Direction.DOWN -> arrayOf(
                    Vector3f(origin).add(0f, 0f, height),
                    Vector3f(origin).add(width, 0f, height),
                    Vector3f(origin).add(width, 0f, 0f),
                    Vector3f(origin)
                )
                Direction.UP -> arrayOf(
                    Vector3f(origin).add(0f, 1f, 0f),
                    Vector3f(origin).add(width, 1f, 0f),
                    Vector3f(origin).add(width, 1f, height),
                    Vector3f(origin).add(0f, 1f, height)
                )
            }

            return LightFace(
                pos,
                vertices[0], vertices[1], vertices[2], vertices[3],
                Vector2f(sprite.u0, sprite.v0),
                Vector2f(sprite.u1, sprite.v0),
                Vector2f(sprite.u1, sprite.v1),
                Vector2f(sprite.u0, sprite.v1)
            )
        }
    }
}