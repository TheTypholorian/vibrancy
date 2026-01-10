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

open class ShadowGreedyMesher(val box: BlockBox) : ShadowMesher {
    val grid: Array<Array<Array<Voxel?>>> = Array(box.max.x - box.min.x + 1) { x ->
        Array(box.max.y - box.min.y + 1) { y ->
            Array(box.max.z - box.min.z + 1) { z ->
                null
            }
        }
    }
    val allVoxels = LinkedList<Voxel>()
    val nonGreedy = LinkedList<LightFace>()

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
        } else if (predicate.isInRange(pos)) {
            ShadowMesher.collectLightFaces(state, level, pos, predicate, nonGreedy::add)
        }
    }

    override fun finish(predicate: FaceCastingPredicate, level: Level, out: Consumer<LightFace>) {
        var start: BlockPos? = null
        var length = 0
        var sprite: TextureAtlasSprite? = null
        val faces = HashMap<Direction, MutableMap<BlockPos, LightFace>>()

        fun start(pos: BlockPos, quad: BakedQuad) {
            start = pos
            length = 1
            sprite = quad.sprite
        }

        fun end(direction: Direction) {
            if (length > 0) {
                faces.computeIfAbsent(direction) { HashMap() }.put(
                    start!!,
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

        val extraGreedy = LinkedList<LightFace>()
        val removed = LinkedList<LightFace>()

        for (entry in faces) {
            val sideAxis = if (entry.key.axis == Direction.Axis.Y) Direction.Axis.X else Direction.Axis.Y

            entry.value.entries.forEach { entry1 ->
                if (removed.contains(entry1.value)) {
                    return@forEach
                }

                val doubleGreedy = LinkedList(listOf(entry1.value))
                var pos = entry1.value.blockPos!!

                while (true) {
                    pos = pos.relative(sideAxis, 1)

                    val other = entry.value[pos]

                    if (other == null) {
                        break
                    }

                    if (entry.key.axis == Direction.Axis.Y) {
                        if (other.height != entry1.value.height) {
                            break
                        }
                    } else {
                        if (other.width != entry1.value.width) {
                            break
                        }
                    }

                    doubleGreedy.add(other)
                }

                pos = entry1.value.blockPos!!

                while (true) {
                    pos = pos.relative(sideAxis, -1)

                    val other = entry.value[pos]

                    if (other == null) {
                        break
                    }

                    if (entry.key.axis == Direction.Axis.Y) {
                        if (other.height != entry1.value.height) {
                            break
                        }
                    } else {
                        if (other.width != entry1.value.width) {
                            break
                        }
                    }

                    doubleGreedy.addFirst(other)
                }

                if (doubleGreedy.size > 1) {
                    removed.addAll(doubleGreedy)
                    extraGreedy.add(
                        if (entry.key.axis == Direction.Axis.Y) {
                            entry.key.createFace(
                                doubleGreedy.first().blockPos!!,
                                entry1.value.sprite!!,
                                width = doubleGreedy.size,
                                height = entry1.value.height
                            )
                        } else {
                            entry.key.createFace(
                                doubleGreedy.first().blockPos!!,
                                entry1.value.sprite!!,
                                width = entry1.value.width,
                                height = doubleGreedy.size
                            )
                        }
                    )
                }
            }
        }

        // TODO double greedy mesh

        extraGreedy.forEach(out::accept)

        for (map in faces.values) {
            for (face in map.values) {
                if (!removed.contains(face)) {
                    out.accept(face)
                }
            }
        }

        for (voxel in allVoxels) {
            if (predicate.isInRange(voxel.pos)) {
                for (direction in Direction.entries) {
                    voxel.quads[direction.ordinal]?.let { quad ->
                        out.accept(quad.toLightFace(0f, 0f, 0f, voxel.pos))
                    }
                }
            }
        }

        for (face in nonGreedy) {
            out.accept(face)
        }
    }

    class Voxel(val pos: BlockPos, val solid: Boolean) {
        val quads = arrayOfNulls<BakedQuad?>(Direction.entries.size)
    }
}