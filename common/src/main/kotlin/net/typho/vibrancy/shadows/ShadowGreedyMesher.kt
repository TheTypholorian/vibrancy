package net.typho.vibrancy.shadows

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.opengl.util.TexturedQuad
import net.typho.big_shot_lib.api.client.util.BlockRenderSettings
import net.typho.big_shot_lib.api.client.util.BlockRenderSettingsUtil
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.util.TextureCoordinates
import org.joml.Vector3f
import java.util.*
import java.util.function.Consumer

open class ShadowGreedyMesher(val box: BlockBox) : ShadowMesher {
    val grid: Array<Array<Array<Voxel?>>> = Array(box.sizeX()) {
        Array(box.sizeY()) {
            Array(box.sizeZ()) {
                null
            }
        }
    }
    val nonGreedy = ArrayList<LightFace>()
    val lightFaces = ArrayList<LightFace>()

    fun shouldGreedyMesh(
        state: BlockState,
        level: Level,
        pos: BlockPos
    ) = BlockUtil.INSTANCE.isSolidRender(state, pos, level) && BlockRenderSettingsUtil.INSTANCE.getBlockSettings(state) == BlockRenderSettings.SOLID

    override fun submit(
        manager: LightManager,
        level: Level,
        pos: BlockPos,
        random: RandomSource,
        predicate: ShadowPredicate
    ) {
        val block = level.getBlockState(pos)

        if (predicate.shouldCastBlock(block, level, pos)) {
            val greedy = shouldGreedyMesh(block, level, pos)
            val light = predicate.isInLightRange(pos)
            val shadow = predicate.isInShadowRange(pos)
            val voxel = if (greedy) Voxel(pos) else null

            if (light || shadow || greedy) {
                ShadowMesher.collectLightFaces(manager, block, level, pos, predicate) { dir, face ->
                    if (light) {
                        lightFaces.add(face)
                    }

                    if (voxel != null && dir != null) {
                        if (voxel.quads[dir.ordinal] == null) {
                            voxel.quads[dir.ordinal] = face.quad
                        } else {
                            nonGreedy.add(face)
                        }
                    } else if (shadow) {
                        nonGreedy.add(face)
                    }
                }
            }

            grid[pos.x - box.min.x][pos.y - box.min.y][pos.z - box.min.z] = voxel
        }
    }

    override fun finish(
        manager: LightManager,
        predicate: ShadowPredicate,
        level: Level,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    ) {
        var start: BlockPos? = null
        var length = 0
        var texture: TextureCoordinates? = null
        val currentVoxels = ArrayList<Voxel>()

        fun start(pos: BlockPos, quad: TexturedQuad) {
            start = pos
            length = 1
            texture = TextureCoordinates(
                quad.uv1,
                quad.uv2,
                quad.uv3,
                quad.uv4,
            )
            currentVoxels.clear()
        }

        fun end(direction: Direction, axis: Direction.Axis) {
            if (length > 1) {
                shadowOut.accept(
                    if (axis != Direction.Axis.X && (axis == Direction.Axis.Y || direction.axis == Direction.Axis.Y)) {
                        direction.createFace(
                            start!!,
                            texture!!,
                            height = length
                        )
                    } else {
                        direction.createFace(
                            start!!,
                            texture!!,
                            width = length
                        )
                    }
                )

                for (voxel in currentVoxels) {
                    voxel.quads[direction.ordinal] = null
                }
            }

            start = null
            length = 0
            texture = null

            currentVoxels.clear()
        }

        fun mesh(voxel: Voxel?, direction: Direction, axis: Direction.Axis) {
            if (voxel != null) {
                val quad = voxel.quads[direction.ordinal]

                if (quad == null) {
                    end(direction, axis)
                } else {
                    if (length == 0) {
                        start(voxel.pos, quad)
                    } else {
                        length++
                    }

                    currentVoxels.add(voxel)
                }
            } else {
                end(direction, axis)
            }
        }

        for (x in box.min.x..box.max.x) {
            for (y in box.min.y..box.max.y) {
                val directions = arrayOf(Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST)

                for (direction in directions) {
                    for (z in box.min.z..box.max.z) {
                        mesh(grid[x - box.min.x][y - box.min.y][z - box.min.z], direction, Direction.Axis.Z)
                    }

                    end(direction, Direction.Axis.Z)
                }
            }
        }

        for (z in box.min.z..box.max.z) {
            for (y in box.min.y..box.max.y) {
                val directions = arrayOf(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH)

                for (direction in directions) {
                    for (x in box.min.x..box.max.x) {
                        mesh(grid[x - box.min.x][y - box.min.y][z - box.min.z], direction, Direction.Axis.X)
                    }

                    end(direction, Direction.Axis.X)
                }
            }
        }

        for (x in box.min.x..box.max.x) {
            for (z in box.min.z..box.max.z) {
                val directions = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

                for (direction in directions) {
                    for (y in box.min.y..box.max.y) {
                        mesh(grid[x - box.min.x][y - box.min.y][z - box.min.z], direction, Direction.Axis.Y)
                    }

                    end(direction, Direction.Axis.Y)
                }
            }
        }

        for (arrays in grid) {
            for (voxels in arrays) {
                for (voxel in voxels) {
                    if (voxel != null && predicate.isInShadowRange(voxel.pos)) {
                        for (direction in Direction.entries) {
                            voxel.quads[direction.ordinal]?.let { quad ->
                                shadowOut.accept(
                                    LightFace(
                                        voxel.pos,
                                        quad,
                                        1,
                                        1
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        nonGreedy.forEach(shadowOut)
        lightFaces.forEach(lightOut)
    }

    @JvmRecord
    data class Voxel(
        @JvmField
        val pos: BlockPos,
        @JvmField
        val quads: Array<TexturedQuad?> = arrayOfNulls<TexturedQuad?>(Direction.entries.size)
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Voxel

            if (pos != other.pos) return false
            if (!quads.contentEquals(other.quads)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = pos.hashCode()
            result = 31 * result + quads.contentHashCode()
            return result
        }
    }

    companion object {
        @JvmStatic
        fun Direction.createFace(
            pos: BlockPos,
            texture: TextureCoordinates,
            width: Int = 1,
            height: Int = 1
        ): LightFace {
            val w = width.toFloat()
            val h = height.toFloat()

            val origin = Vec3.atLowerCornerOf(pos).toVector3f()
            val vertices: Array<Vector3f> = when (this) {
                Direction.NORTH -> arrayOf(
                    Vector3f(origin),
                    Vector3f(origin).add(w, 0f, 0f),
                    Vector3f(origin).add(w, h, 0f),
                    Vector3f(origin).add(0f, h, 0f)
                )
                Direction.SOUTH -> arrayOf(
                    Vector3f(origin).add(w, 0f, 1f),
                    Vector3f(origin).add(0f, 0f, 1f),
                    Vector3f(origin).add(0f, h, 1f),
                    Vector3f(origin).add(w, h, 1f)
                )
                Direction.WEST -> arrayOf(
                    Vector3f(origin).add(0f, 0f, w),
                    Vector3f(origin),
                    Vector3f(origin).add(0f, h, 0f),
                    Vector3f(origin).add(0f, h, w)
                )
                Direction.EAST -> arrayOf(
                    Vector3f(origin).add(1f, 0f, 0f),
                    Vector3f(origin).add(1f, 0f, w),
                    Vector3f(origin).add(1f, h, w),
                    Vector3f(origin).add(1f, h, 0f)
                )
                Direction.DOWN -> arrayOf(
                    Vector3f(origin).add(0f, 0f, h),
                    Vector3f(origin).add(w, 0f, h),
                    Vector3f(origin).add(w, 0f, 0f),
                    Vector3f(origin)
                )
                Direction.UP -> arrayOf(
                    Vector3f(origin).add(0f, 1f, 0f),
                    Vector3f(origin).add(w, 1f, 0f),
                    Vector3f(origin).add(w, 1f, h),
                    Vector3f(origin).add(0f, 1f, h)
                )
            }

            return LightFace(
                pos,
                TexturedQuad(
                    vertices[0],
                    vertices[1],
                    vertices[2],
                    vertices[3],
                    texture.uv0,
                    texture.uv1,
                    texture.uv2,
                    texture.uv3,
                    -1
                ),
                width,
                height
            )
        }
    }
}