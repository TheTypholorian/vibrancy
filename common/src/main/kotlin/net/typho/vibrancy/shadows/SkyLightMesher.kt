package net.typho.vibrancy.shadows

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.vibrancy.LightManager

class SkyLightMesher(
    @JvmField
    val pos: ChunkPos
) : ShadowMesher {
    override fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        predicate: LightFacePredicate,
        out: (face: LightFace) -> Unit
    ) {
        val faces = arrayListOf<LightFace>()

        fun collect(pos: AbstractVec3<Int>, state: BlockState) {
            ShadowMesher.collectLightFaces(
                manager,
                state,
                level,
                pos,
                atlas,
                { predicate.shouldCastFace(it, level, pos, state) },
                { dir, face -> faces.add(face) }
            )
        }

        val chunk = level.getChunk(pos.x, pos.z)

        repeat(16) { x ->
            repeat(16) { z ->
                val height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                var y = height

                while (y > chunk.minBuildHeight) {
                    val pos = NeoVec3i(x + pos.minBlockX, y, z + pos.minBlockZ)
                    val state = chunk.getBlockState(pos.blockPos)

                    if (predicate.shouldCastBlock(level, pos, state)) {
                        collect(pos, state)
                    }

                    if (!state.propagatesSkylightDown(level, pos.blockPos) && state.block !is LeavesBlock) { // TODO
                        break
                    }

                    y--
                }
            }
        }

        /*
        while (cursors.isNotEmpty()) {
            val cursor = cursors.first()
            cursors.remove(cursor)

            collect(cursor, level.getBlockState(cursor))

            for (direction in NeoDirection.entries) {
                val pos = cursor.relative(direction)

                if (direction.isPointingTowardsInclusive(this.pos, cursor) && checked.add(pos)) {
                    val state = level.getBlockState(pos)

                    if (predicate.shouldCastBlock(level, pos, state) && predicate.isInLightRange(pos)) {
                        if (BlockUtil.INSTANCE.isSolidRender(state, pos, level)) {
                            collect(pos, state)
                        } else {
                            cursors.add(pos)
                        }
                    }
                }
            }
        }
         */

        faces.forEach {
            out(it)
        }
    }
}