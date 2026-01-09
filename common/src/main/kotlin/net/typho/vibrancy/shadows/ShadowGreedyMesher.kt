package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.shadows.LightFace.Companion.toLightFace
import net.typho.vibrancy.shadows.ShadowMesher.Companion.createFace
import java.util.*
import java.util.function.Consumer

open class ShadowGreedyMesher : ShadowMesher {
    val box: BlockBox
    protected val grid: Array<Array<Array<Voxel?>>>
    protected val allVoxels = LinkedList<Voxel>()
    protected val nonGreedy = LinkedList<LightFace>()

    constructor(box: BlockBox) {
        this.box = box
        grid = Array(box.max.x - box.min.x + 1) { x ->
            Array(box.max.y - box.min.y + 1) { y ->
                Array(box.max.z - box.min.z + 1) { z ->
                    null
                }
            }
        }
    }

    fun shouldGreedyMesh(
        state: BlockState,
        level: Level,
        pos: BlockPos
    ) = state.isSolidRender(level, pos)

    override fun submit(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        random: RandomSource,
        predicate: FaceCastingPredicate
    ) {
        if (shouldGreedyMesh(state, level, pos)) {
            val voxel = Voxel(pos, ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.solid())
            val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)

            for (direction in Direction.entries) {
                if (predicate.shouldCast(direction, level.getBlockState(voxel.pos), level, voxel.pos)) {
                    voxel.quads[direction.ordinal] = model.getQuads(state, direction, random).first()
                }
            }

            grid[pos.x - box.min.x][pos.y - box.min.y][pos.z - box.min.z] = voxel
            allVoxels.add(voxel)
        } else {
            ShadowMesher.collectLightFaces(state, level, pos, predicate, nonGreedy::add)
        }
    }

    override fun finish(predicate: FaceCastingPredicate, level: Level, out: Consumer<LightFace>) {
        var start: BlockPos? = null
        var length = 0
        var sprite: TextureAtlasSprite? = null
        val faces = LinkedList<LightFace>()

        fun start(pos: BlockPos, quad: BakedQuad) {
            start = pos
            length = 1
            sprite = quad.sprite
        }

        fun end(direction: Direction) {
            if (length > 0) {
                faces.add(
                    if (direction.axis == Direction.Axis.Y) {
                        direction.createFace(
                            start!!,
                            sprite!!,
                            height = length
                        )
                    } else {
                        direction.createFace(
                            start!!,
                            sprite!!,
                            width = length
                        )
                    }
                )
            }

            start = null
            length = 0
            sprite = null
        }

        fun mesh(voxel: Voxel, direction: Direction) {
            if (voxel.solid) {
                val quad = voxel.quads[direction.ordinal]

                if (quad == null) {
                    end(direction)
                } else {
                    if (length == 0) {
                        start(voxel.pos, quad)
                    } else {
                        length++
                    }

                    voxel.quads[direction.ordinal] = null
                }
            } else {
                end(direction)
            }
        }

        for (x in box.min.x..box.max.x) {
            for (y in box.min.y..box.max.y) {
                val directions = arrayOf(Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST)

                for (direction in directions) {
                    for (z in box.min.z..box.max.z) {
                        grid[x - box.min.x][y - box.min.y][z - box.min.z]?.let { voxel -> mesh(voxel, direction) }
                    }

                    end(direction)
                }
            }
        }

        for (z in box.min.z..box.max.z) {
            for (y in box.min.y..box.max.y) {
                val directions = arrayOf(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH)

                for (direction in directions) {
                    for (x in box.min.x..box.max.x) {
                        grid[x - box.min.x][y - box.min.y][z - box.min.z]?.let { voxel -> mesh(voxel, direction) }
                    }

                    end(direction)
                }
            }
        }

        for (x in box.min.x..box.max.x) {
            for (z in box.min.z..box.max.z) {
                val directions = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

                for (direction in directions) {
                    for (y in box.min.y..box.max.y) {
                        grid[x - box.min.x][y - box.min.y][z - box.min.z]?.let { voxel -> mesh(voxel, direction) }
                    }

                    end(direction)
                }
            }
        }

        // TODO double greedy mesh

        faces.forEach(out::accept)

        for (voxel in allVoxels) {
            for (direction in Direction.entries) {
                voxel.quads[direction.ordinal]?.let { quad ->
                    out.accept(quad.toLightFace(0f, 0f, 0f, voxel.pos))
                }
            }
        }
    }

    class Voxel(val pos: BlockPos, val solid: Boolean) {
        val quads = arrayOfNulls<BakedQuad?>(Direction.entries.size)
    }
}