package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoVertexData
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.vibrancy.LightManager

// TODO
class SkyLightBlockMeshCollector(
    @JvmField
    val pos: ChunkPos
) : BlockMeshCollector {
    var blockEntities: MutableSet<BlockPos> = hashSetOf()
        private set

    override fun scan(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        predicate: BlockMeshCollector.Predicate
    ): Boolean {
        /*
        val origin = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)
        val blockEntities = hashSetOf<BlockPos>()

        fun collect(pos: BlockPos.MutableBlockPos, state: BlockState) {
            val offset = (NeoVec3i(pos) - origin).plus(0, pos.y and 15.inv(), 0).toFloat()

            BlockMeshCollector.collectLightFaces(
                manager,
                state,
                level,
                pos,
                { face ->
                    face.copyWithOffset(offset.x, offset.y, offset.z) // TODO
                },
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

                pos.set(x + this.pos.minBlockX, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z + this.pos.minBlockZ)

                //? if <1.21.5 {
                while (pos.y >= chunk.minBuildHeight) {
                    //? } else {
                    /*while (y >= chunk.minY) {
                    *///? }
                    val state = chunk.getBlockState(pos)

                    if (consumers.any { it.predicate.shouldCastBlock(level, pos, state) }) {
                        collect(pos, state)
                    } else {
                        break
                    }

                    pos.move(Direction.DOWN)
                }
            }
        }

        this.blockEntities = blockEntities

        return true
         */
        return true
    }

    override fun mesh(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        consumer: BlockMeshCollector.Consumer
    ) {
    }
}