package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.vibrancy.LightManager

class SkyLightBlockMeshCollector(
    @JvmField
    val pos: ChunkPos
) : BlockMeshCollector {
    var blockEntities: MutableSet<BlockPos> = hashSetOf()
        private set

    @Suppress("USELESS_ELVIS")
    override fun submit(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        vararg consumers: BlockMeshCollector.Consumer
    ): Boolean {
        val origin = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)
        val blockEntities = hashSetOf<BlockPos>()

        fun collect(pos: BlockPos.MutableBlockPos, state: BlockState) {
            BlockMeshCollector.collectLightFaces(
                manager,
                state,
                level,
                pos,
                NeoVec3i(pos) - origin,
                atlas,
                true,
                *consumers
            )

            if (level.getBlockEntity(pos) != null) {
                blockEntities.add(pos.immutable())
            }
        }

        val chunk = level.getChunk(pos.x, pos.z)
        val pos = BlockPos.MutableBlockPos()

        repeat(16) { x ->
            repeat(16) { z ->
                if (isCancelled()) {
                    return false
                }

                var y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)

                //? if <1.21.5 {
                while (y >= chunk.minBuildHeight) {
                //? } else {
                /*while (y >= chunk.minY) {
                *///? }
                    pos.set(x + this.pos.minBlockX, y, z + this.pos.minBlockZ)
                    val state = chunk.getBlockState(pos)

                    if (consumers.any { it.predicate.shouldCastBlock(level, pos, state) }) {
                        collect(pos, state)
                    } else {
                        break
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

        this.blockEntities = blockEntities

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