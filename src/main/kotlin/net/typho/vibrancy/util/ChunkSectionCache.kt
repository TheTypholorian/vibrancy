package net.typho.vibrancy.util

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.typho.big_shot_lib.api.math.IRect3

class ChunkSectionCache(
    @JvmField
    val level: Level
) {
    @JvmField
    val sections = hashMapOf<SectionPos, LevelChunkSection>()

    operator fun get(pos: SectionPos) = sections.computeIfAbsent(pos) { pos ->
        level.getChunk(pos.x, pos.z).let { it.getSection(it.getSectionIndexFromSectionY(pos.y)) }
    }

    operator fun get(pos: BlockPos) = if (level.isOutsideBuildHeight(pos)) {
        Blocks.VOID_AIR.defaultBlockState()
    } else {
        get(SectionPos.of(pos)).getBlockState(pos.x and 15, pos.y and 15, pos.z and 15)
    }

    fun get(min: BlockPos, max: BlockPos): Iterator<Pair<BlockPos, BlockState>> {
        val min = min.atY(min.y.coerceAtLeast(level.minY).coerceAtMost(level.maxY))
        val max = max.atY(max.y.coerceAtLeast(level.minY).coerceAtMost(level.maxY))

        val pos = BlockPos.MutableBlockPos().set(min)
        pos.z--

        return object : Iterator<Pair<BlockPos, BlockState>> {
            override fun hasNext(): Boolean {
                return pos.x < max.x
            }

            override fun next(): Pair<BlockPos, BlockState> {
                pos.z++

                if (pos.z > max.z) {
                    pos.z = min.z
                    pos.y++

                    if (pos.y > max.y) {
                        pos.y = min.y
                        pos.x++
                    }
                }

                val state = get(pos) //sections.get(SectionPos.blockToSectionCoord(pos.x), SectionPos.blockToSectionCoord(pos.y), SectionPos.blockToSectionCoord(pos.z))
                    //.getBlockState(SectionPos.sectionRelative(pos.x), SectionPos.sectionRelative(pos.y), SectionPos.sectionRelative(pos.z))

                //println("section $pos $state")

                return pos to state
            }
        }
    }

    operator fun get(box: BlockBox) = get(box.min, box.max)

    operator fun get(box: IRect3<Int>) = get(box.min.toBlockPos(), box.max.toBlockPos())
}