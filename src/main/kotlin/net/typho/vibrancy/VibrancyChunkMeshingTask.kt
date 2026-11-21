package net.typho.vibrancy

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior
import net.caffeinemc.mods.sodium.client.world.LevelSlice
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection
import net.minecraft.core.BlockPos
import org.joml.Vector3dc

class VibrancyChunkMeshingTask(
    render: RenderSection,
    buildTime: Int,
    absoluteCameraPos: Vector3dc,
    renderContext: ChunkRenderContext,
    sortBehavior: SortBehavior,
    forceSort: Boolean,
    blockingTask: Boolean,
    @JvmField
    val lights: List<BlockPos> = getLights(renderContext.sections[LevelSlice.getLocalSectionIndex(1, 1, 1)])
) : ChunkBuilderMeshingTask(render, buildTime, absoluteCameraPos, renderContext, sortBehavior, forceSort, blockingTask) {
    companion object {
        @JvmStatic
        fun getLights(section: ClonedChunkSection): List<BlockPos> {
            val blocks = section.blockData ?: return listOf()

            return if (blocks.maybeHas { it.lightEmission != 0 }) {
                buildList {
                    repeat(16) { x ->
                        repeat(16) { y ->
                            repeat(16) { z ->
                                val state = blocks.get(x, y, z)

                                if (state.lightEmission != 0) {
                                    add(BlockPos(x, y, z))
                                }
                            }
                        }
                    }
                }
            } else {
                listOf()
            }
        }
    }
}