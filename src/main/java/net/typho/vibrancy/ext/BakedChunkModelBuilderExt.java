package net.typho.vibrancy.ext;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

public interface BakedChunkModelBuilderExt {
    void vibrancy$setInfo(ChunkBuildBuffers buffers, TerrainRenderPass pass);
}
