package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.typho.vibrancy.ext.ChunkBuildBuffersHolderExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BakedChunkModelBuilder.class)
public class BakedChunkModelBuilderMixin implements ChunkBuildBuffersHolderExt {
    @Shadow
    @Final
    private ChunkMeshBufferBuilder[] vertexBuffers;
    @Unique
    private ChunkBuildBuffers vibrancy$chunkBuildBuffers;

    @Override
    public ChunkBuildBuffers vibrancy$getChunkBuildBuffers() {
        return vibrancy$chunkBuildBuffers;
    }

    @Override
    public void vibrancy$setChunkBuildBuffers(ChunkBuildBuffers buffers) {
        vibrancy$chunkBuildBuffers = buffers;

        for (ChunkMeshBufferBuilder buffer : vertexBuffers) {
            ((ChunkBuildBuffersHolderExt) buffer).vibrancy$setChunkBuildBuffers(buffers);
        }
    }
}
