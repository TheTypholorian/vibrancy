package net.typho.vibrancy.ext;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

import java.util.List;

public interface ChunkMeshBufferBuilderExt {
    List<Integer> vibrancy$getIndices();

    void vibrancy$setInfo(ChunkBuildBuffers buffers, TerrainRenderPass pass, ModelQuadFacing facing);
}
