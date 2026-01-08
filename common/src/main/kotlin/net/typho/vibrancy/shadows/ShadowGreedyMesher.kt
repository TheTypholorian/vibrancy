package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

open class ShadowGreedyMesher : ShadowMesher {
    protected var box: BlockBox
    protected var grid: Array<Array<Array<Voxel>>>

    constructor(box: BlockBox) {
        this.box = box
        grid = Array(box.max.x - box.min.x) { x ->
            Array(box.max.y - box.min.y) { y ->
                Array(box.max.z - box.min.z) { z ->
                    Voxel()
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
        pos: BlockPos
    ) {
    }

    override fun upload(vbo: VertexBuffer) {
    }

    inner class Voxel {
        //val faces = Array<List<LightFace>>(Direction.entries.size + 1) { LinkedList() }

        //fun getFaces(direction: Direction?) = faces[direction?.ordinal ?: Direction.entries.size]
    }
}