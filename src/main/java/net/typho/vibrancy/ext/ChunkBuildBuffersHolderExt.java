package net.typho.vibrancy.ext;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;

public interface ChunkBuildBuffersHolderExt {
    ChunkBuildBuffers vibrancy$getChunkBuildBuffers();

    void vibrancy$setChunkBuildBuffers(ChunkBuildBuffers buffers);
}
