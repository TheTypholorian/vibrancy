package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.typho.vibrancy.ext.ChunkBuildBuffersExt;
import net.typho.vibrancy.ext.ChunkBuildBuffersHolderExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMeshBufferBuilder.class)
public class ChunkMeshBufferBuilderMixin implements ChunkBuildBuffersHolderExt {
    @Unique
    private ChunkBuildBuffers vibrancy$chunkBuildBuffers;

    @Override
    public ChunkBuildBuffers vibrancy$getChunkBuildBuffers() {
        return vibrancy$chunkBuildBuffers;
    }

    @Override
    public void vibrancy$setChunkBuildBuffers(ChunkBuildBuffers buffers) {
        vibrancy$chunkBuildBuffers = buffers;
    }

    @Inject(
            method = "push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void push(ChunkVertexEncoder.Vertex[] vertices, int materialBits, CallbackInfo ci) {
        for (Vec3i light : ((ChunkBuildBuffersExt) vibrancy$chunkBuildBuffers).vibrancy$getLightSources()) {
            System.out.println(light + " " + new Vec3(vertices[0].x, vertices[0].y, vertices[0].z));
            if (Vec3.atLowerCornerOf(light).distanceToSqr(new Vec3(vertices[0].x, vertices[0].y, vertices[0].z)) < (10 * 10)) {
                ci.cancel();
                return;
            }
        }
    }
}
