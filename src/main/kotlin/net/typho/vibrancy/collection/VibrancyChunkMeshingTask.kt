package net.typho.vibrancy.collection

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext
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
    val light: BlockPos
) : ChunkBuilderMeshingTask(render, buildTime, absoluteCameraPos, renderContext, sortBehavior, forceSort, blockingTask)