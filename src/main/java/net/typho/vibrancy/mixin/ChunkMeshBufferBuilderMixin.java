package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.typho.vibrancy.ext.ChunkBuildBuffersExt;
import net.typho.vibrancy.ext.ChunkMeshBufferBuilderExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChunkMeshBufferBuilder.class)
public class ChunkMeshBufferBuilderMixin implements ChunkMeshBufferBuilderExt {
    @Shadow
    private int vertexCount;
    @Unique
    private final List<Integer> vibrancy$indices = new ArrayList<>();
    @Unique
    private ChunkBuildBuffers vibrancy$chunkBuildBuffers;
    @Unique
    private TerrainRenderPass vibrancy$renderPass;
    @Unique
    private ModelQuadFacing vibrancy$facing;

    @Override
    public List<Integer> vibrancy$getIndices() {
        return vibrancy$indices;
    }

    @Override
    public void vibrancy$setInfo(ChunkBuildBuffers buffers, TerrainRenderPass pass, ModelQuadFacing facing) {
        vibrancy$chunkBuildBuffers = buffers;
        vibrancy$renderPass = pass;
        vibrancy$facing = facing;
    }

    @Inject(
            method = "push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V",
            at = @At("HEAD")
    )
    private void push(ChunkVertexEncoder.Vertex[] vertices, int materialBits, CallbackInfo ci) {
        for (Vec3i light : ((ChunkBuildBuffersExt) vibrancy$chunkBuildBuffers).vibrancy$getLightSources()) {
            if (Vec3.atLowerCornerOf(light).distanceToSqr(new Vec3(vertices[0].x, vertices[0].y, vertices[0].z)) < (6 * 6)) {
                vibrancy$indices.add(vertexCount);
                return;
            }
        }
    }

    @Inject(
            method = "destroy",
            at = @At("TAIL")
    )
    private void destroy(CallbackInfo ci) {
        vibrancy$indices.clear();
    }
}
