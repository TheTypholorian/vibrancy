package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.typho.vibrancy.ext.ChunkBuildBuffersExt;
import net.typho.vibrancy.ext.ChunkMeshBufferBuilderExt;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

@Mixin(ChunkMeshBufferBuilder.class)
public abstract class ChunkMeshBufferBuilderMixin implements ChunkMeshBufferBuilderExt {
    @Shadow
    private int vertexCount;
    @Unique
    private MemorySegment vibrancy$indices = null;
    @Unique
    private int vibrancy$indexCapacity = 4 * 6 * 128;
    @Unique
    private int vibrancy$indexCount = 0;
    @Unique
    private ChunkBuildBuffers vibrancy$chunkBuildBuffers;
    @Unique
    private TerrainRenderPass vibrancy$renderPass;
    @Unique
    private ModelQuadFacing vibrancy$facing;

    @Override
    public int vibrancy$overlayIndicesCount() {
        return vibrancy$indexCount;
    }

    @Override
    public ByteBuffer vibrancy$sliceOverlayIndices() {
        if (vibrancy$indexCount == 0) {
            throw new IllegalStateException("No index data in vibrancy overlay buffer");
        }

        return vibrancy$indices.asSlice(0, 4 * vibrancy$indexCount).asByteBuffer();
    }

    @Override
    public void vibrancy$setInfo(ChunkBuildBuffers buffers, TerrainRenderPass pass, ModelQuadFacing facing) {
        vibrancy$chunkBuildBuffers = buffers;
        vibrancy$renderPass = pass;
        vibrancy$facing = facing;
    }

    @Unique
    private void vibrancy$ensureIndicesCapacity(int indexCount) {
        if (vibrancy$indexCount + indexCount >= vibrancy$indexCapacity) {
            vibrancy$growIndices(indexCount);
        }
    }

    @Unique
    private void vibrancy$growIndices(int indexCount) {
        vibrancy$reallocateIndices(Math.max(vibrancy$indexCapacity * 2, vibrancy$indexCapacity + indexCount));
    }

    @Unique
    private void vibrancy$reallocateIndices(int indexCount) {
        vibrancy$indices = MemorySegment.ofAddress(MemoryUtil.nmemRealloc(vibrancy$indices == null ? 0L : vibrancy$indices.address(), indexCount * 4)).reinterpret(indexCount * 4);
        vibrancy$indexCapacity = indexCount;
    }

    @Inject(
            method = "push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V",
            at = @At("HEAD")
    )
    private void push(ChunkVertexEncoder.Vertex[] vertices, int materialBits, CallbackInfo ci) {
        for (Vec3i light : ((ChunkBuildBuffersExt) vibrancy$chunkBuildBuffers).vibrancy$getLightSources()) {
            if (Vec3.atLowerCornerOf(light).distanceToSqr(new Vec3(vertices[0].x, vertices[0].y, vertices[0].z)) < (6 * 6)) {
                var baseIndex = vertexCount;

                vibrancy$ensureIndicesCapacity(6);
                var ptr = vibrancy$indices.address() + ((long) vibrancy$indexCount * 4);

                MemoryIntrinsics.putInt(ptr, baseIndex);
                MemoryIntrinsics.putInt(ptr + 4, baseIndex + 1);
                MemoryIntrinsics.putInt(ptr + 8, baseIndex + 2);
                MemoryIntrinsics.putInt(ptr + 12, baseIndex + 2);
                MemoryIntrinsics.putInt(ptr + 16, baseIndex + 3);
                MemoryIntrinsics.putInt(ptr + 20, baseIndex);

                vibrancy$indexCount += 6;

                return;
            }
        }
    }

    @Inject(
            method = "start",
            at = @At("TAIL")
    )
    private void start(CallbackInfo ci) {
        vibrancy$indexCount = 0;
        vibrancy$reallocateIndices(4 * 6 * 128);
    }

    @Inject(
            method = "destroy",
            at = @At("TAIL")
    )
    private void destroy(CallbackInfo ci) {
        if (vibrancy$indices != null) {
            MemoryUtil.nmemFree(vibrancy$indices.address());
        }

        vibrancy$indices = null;
    }
}
