package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
        predicate: FaceCastingPredicate
    ) {
        if (shouldGreedyMesh(state, level, pos)) {
            val voxel = Voxel(pos, ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.solid())
            val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)

            for (direction in Direction.entries) {
                voxel.faces[direction.ordinal] = model.getQuads(state, direction, level.random).first()
            }

            grid[pos.x - box.min.x][pos.y - box.min.y][pos.z - box.min.z] = voxel
            allVoxels.add(voxel)
        } else {
            ShadowMesher.collectLightFaces(state, level, pos, predicate, nonGreedy::add)
        }
    }

    override fun finish(out: Consumer<LightFace>) {
        for (x in box.min.x..box.max.x) {
            val grid1 = grid[x - box.min.x]

            for (y in box.min.y..box.max.y) {
                val grid2 = grid1[y - box.min.y]

                val directions = arrayOf(Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST)

                for (direction in directions) {
                    var startZ = -1
                    var length = 0
                    var sprite: TextureAtlasSprite? = null
                    var solid = false

                    fun start(z: Int, quad: BakedQuad, s: Boolean) {
                        startZ = z
                        length = 1
                        sprite = quad.sprite
                        solid = s
                    }

                    fun end() {
                        if (length > 0) {
                            if (direction.axis == Direction.Axis.Y) {
                                out.accept(direction.createFace(
                                    BlockPos(x, y, startZ),
                                    sprite!!,
                                    height = length.toFloat()
                                ))
                            } else {
                                out.accept(direction.createFace(
                                    BlockPos(x, y, startZ),
                                    sprite!!,
                                    width = length.toFloat()
                                ))
                            }
                        }

                        startZ = -1
                        length = 0
                        sprite = null
                        solid = false
                    }

                    fun shouldContinue(quad: BakedQuad, s: Boolean): Boolean {
                        return (solid && s) || sprite!! == quad.sprite
                    }

                    for (z in box.min.z..box.max.z) {
                        grid2[z - box.min.z]?.let { voxel ->
                            voxel.faces[direction.ordinal]?.let { quad ->
                                if (startZ == -1) {
                                    start(z, quad, voxel.solid)
                                } else if (shouldContinue(quad, voxel.solid)) {
                                    length++
                                } else {
                                    end()
                                    start(z, quad, voxel.solid)
                                }

                                voxel.faces[direction.ordinal] = null
                            }
                        }
                    }

                    end()
                }
            }
        }

        for (voxel in allVoxels) {
            for (direction in Direction.entries) {
                voxel.faces[direction.ordinal]?.let { face ->
                    out.accept(face.toLightFace(0f, 0f, 0f, voxel.pos))
                }
            }
        }
    }

    class Voxel(val pos: BlockPos, val solid: Boolean) {
        val faces = arrayOfNulls<BakedQuad?>(Direction.entries.size)
    }
}