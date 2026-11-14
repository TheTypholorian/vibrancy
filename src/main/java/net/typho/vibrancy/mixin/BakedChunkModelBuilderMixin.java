package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.typho.vibrancy.ext.BakedChunkModelBuilderExt;
import net.typho.vibrancy.ext.ChunkMeshBufferBuilderExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BakedChunkModelBuilder.class)
public class BakedChunkModelBuilderMixin implements BakedChunkModelBuilderExt {
    @Shadow
    @Final
    private ChunkMeshBufferBuilder[] vertexBuffers;

    @Override
    public void vibrancy$setInfo(ChunkBuildBuffers buffers, TerrainRenderPass pass) {
        for (int i = 0; i < vertexBuffers.length; i++) {
            ((ChunkMeshBufferBuilderExt) vertexBuffers[i]).vibrancy$setInfo(buffers, pass, ModelQuadFacing.values()[i]);
        }
    }
}
