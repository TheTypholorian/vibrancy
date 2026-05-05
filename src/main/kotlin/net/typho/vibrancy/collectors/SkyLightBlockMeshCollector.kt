package net.typho.vibrancy.collectors

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.typho.big_shot_lib.api.client.rendering.util.BlockChunkLayer
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager

class SkyLightBlockMeshCollector(
    @JvmField
    val pos: ChunkPos
) : BlockMeshCollector {
    override fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        vararg consumers: BlockMeshCollector.Consumer
    ) {
        fun collect(pos: IVec3<Int>, state: BlockState) {
            BlockMeshCollector.collectLightFaces(
                manager,
                state,
                level,
                pos,
                pos,
                atlas,
                true,
                *consumers
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

                    if (consumers.any { it.predicate.shouldCastBlock(level, pos, state) }) {
                        collect(pos, state)
                    }

                    if (!state.propagatesSkylightDown(level, pos.blockPos)) {
                        if (
                            level.getBrightness(LightLayer.SKY, pos.blockPos.north()) <= 0 &&
                            level.getBrightness(LightLayer.SKY, pos.blockPos.south()) <= 0 &&
                            level.getBrightness(LightLayer.SKY, pos.blockPos.east()) <= 0 &&
                            level.getBrightness(LightLayer.SKY, pos.blockPos.west()) <= 0
                        ) {
                            break
                        }
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
    }
}