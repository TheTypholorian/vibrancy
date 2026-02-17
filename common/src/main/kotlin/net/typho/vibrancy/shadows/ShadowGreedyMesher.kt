package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.shadows.LightFace.Companion.toLightFace
import net.typho.vibrancy.shadows.ShadowMesher.Companion.createFace
import net.typho.vibrancy.util.TextureCoordinates
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

    @Suppress("DEPRECATION")
    fun shouldGreedyMesh(
        state: BlockState,
        level: Level,
        pos: BlockPos
    ) = state.isSolid

    @Suppress("DEPRECATION")
    override fun submit(
        manager: LightManager,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        random: RandomSource,
        predicate: ShadowPredicate
    ) {
        if (predicate.shouldCastBlock(state, level, pos) && !state.isAir) {
            if (shouldGreedyMesh(state, level, pos)) {
                val voxel = Voxel(pos, ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.solid())
                val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)

                for (direction in Direction.entries) {
                    if (predicate.shouldCastFace(direction, level.getBlockState(voxel.pos), level, voxel.pos)) {
                        val quads = model.getQuads(state, direction, random)

                        if (quads.isNotEmpty()) {
                            voxel.quads[direction.ordinal] = quads.first()
                        }
                    }
                }

                grid[pos.x - box.min.x][pos.y - box.min.y][pos.z - box.min.z] = voxel
                allVoxels.add(voxel)
            } else if (predicate.isInRange(pos)) {
                ShadowMesher.collectLightFaces(manager, state, level, pos, predicate, nonGreedy::add)
            }
        }
    }

    override fun finish(manager: LightManager, predicate: ShadowPredicate, level: Level, out: Consumer<LightFace>) {
        var start: BlockPos? = null
        var length = 0
        var texture: TextureCoordinates? = null
        val faces = HashMap<Direction, MutableMap<BlockPos, LightFace>>()
        val currentVoxels = LinkedList<Voxel>()

        fun start(pos: BlockPos, quad: BakedQuad) {
            start = pos
            length = 1
            texture = TextureCoordinates(quad.sprite)
            currentVoxels.clear()
        }

        fun end(direction: Direction, axis: Direction.Axis) {
            if (length > 1) {
                faces.computeIfAbsent(direction) { HashMap() }.put(
                    start!!,
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
            if (voxel != null && voxel.solid) {
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

                    //if (length >= Vibrancy.config.forNerds.maxGreedyMeshSectionWidth.get()) {
                    //    end(direction, axis)
                    //}
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

        //if (Vibrancy.config.forNerds.useExtraGreedyMeshing.get()) {
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

                    while (true) {//doubleGreedy.size < Vibrancy.config.forNerds.maxGreedyMeshSectionWidth.get()) {
                        pos = pos.relative(sideAxis, 1)

                        val other = entry.value[pos] ?: break

                        if (removed.contains(other)) {
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

                    while (true) {//doubleGreedy.size < Vibrancy.config.forNerds.maxGreedyMeshSectionWidth.get()) {
                        pos = pos.relative(sideAxis, -1)

                        val other = entry.value[pos] ?: break

                        if (removed.contains(other)) {
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
                                    entry1.value.texture,
                                    width = doubleGreedy.size,
                                    height = entry1.value.height
                                )
                            } else {
                                entry.key.createFace(
                                    doubleGreedy.first().blockPos!!,
                                    entry1.value.texture,
                                    width = entry1.value.width,
                                    height = doubleGreedy.size
                                )
                            }
                        )
                    }
                }
            }

            extraGreedy.forEach(out::accept)

            for (map in faces.values) {
                for (face in map.values) {
                    if (!removed.contains(face)) {
                        out.accept(face)
                    }
                }
            }
        //} else {
        //    for (map in faces.values) {
        //        for (face in map.values) {
        //            out.accept(face)
        //        }
        //    }
        //}

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