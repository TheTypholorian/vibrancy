package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.vibrancy.LightManager

class SkyLightBlockMeshCollector(
    @JvmField
    val pos: ChunkPos
) : BlockMeshCollector {
    @Suppress("USELESS_ELVIS")
    override fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        vararg consumers: BlockMeshCollector.Consumer
    ): Boolean {
        fun collect(pos: BlockPos.MutableBlockPos, state: BlockState) {
            BlockMeshCollector.collectLightFaces(
                manager,
                state,
                level,
                pos,
                NeoVec3i(pos),
                atlas,
                true,
                *consumers
            )
        }

        val chunk = level.getChunk(pos.x, pos.z)
        val pos = BlockPos.MutableBlockPos()
        val lightSources = chunk.skyLightSources ?: return false

        repeat(16) { x ->
            repeat(16) { z ->
                var y = lightSources.getLowestSourceY(x, z)

                while (y >= chunk.minBuildHeight) {
                    pos.set(x + this.pos.minBlockX, y, z + this.pos.minBlockZ)
                    val state = chunk.getBlockState(pos)

                    if (consumers.any { it.predicate.shouldCastBlock(level, pos, state) }) {
                        collect(pos, state)
                    }

                    /*
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
                     */

                    y--
                }
            }
        }

        return true

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