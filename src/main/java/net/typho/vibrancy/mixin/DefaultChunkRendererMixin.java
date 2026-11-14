package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlTexelBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlTessellation;
import net.caffeinemc.mods.sodium.client.gl.tessellation.TessellationBinding;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.mixin.core.CommandEncoderAccessor;
import net.caffeinemc.mods.sodium.mixin.core.GlCommandEncoderAccessor;
import net.caffeinemc.mods.sodium.mixin.core.GpuDeviceAccessor;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.ext.DeviceResourcesExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(DefaultChunkRenderer.class)
public abstract class DefaultChunkRendererMixin extends ShaderChunkRenderer {
    @Shadow
    private static void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, CameraTransform camera) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static void executeDrawBatch(CommandList commandList, GlTessellation tessellation, MultiDrawBatch batch) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static void fillCommandBuffer(MultiDrawBatch batch, RenderRegion renderRegion, SectionRenderDataStorage renderDataStorage, ChunkRenderList renderList, CameraTransform camera, TerrainRenderPass pass, boolean useBlockFaceCulling, boolean useIndexedTessellation) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    @Final
    private SharedQuadIndexBuffer sharedIndexBuffer;

    public DefaultChunkRendererMixin(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void render(
            ChunkRenderMatrices matrices,
            CommandList commandList,
            ChunkRenderListIterable renderLists,
            TerrainRenderPass renderPass,
            CameraTransform camera,
            FogParameters parameters,
            boolean indexedRenderingEnabled,
            GpuSampler terrainSampler,
            GpuBufferSlice uniformBuffer,
            GlTexelBuffer sectionTimeInfoTexture,
            CallbackInfo ci,
            @Local(ordinal = 0) boolean useBlockFaceCulling,
            @Local(ordinal = 1) boolean useIndexedTessellation
    ) {
        RenderTarget target = renderPass.getTarget();
        GlStateManager._viewport(0, 0, target.getColorTexture().getWidth(0), target.getColorTexture().getHeight(0));
        GlStateManager._glBindFramebuffer(36160, ((GlTexture)target.getColorTexture()).getFbo(((GlDevice)((GpuDeviceAccessor) RenderSystem.getDevice()).sodium$getBackend()).directStateAccess(), target.getDepthTexture()));
        ((GlCommandEncoderAccessor)((CommandEncoderAccessor)RenderSystem.getDevice().createCommandEncoder()).sodium$getBackend()).sodium$applyPipelineState(renderPass.getPipeline());
        ((GlCommandEncoderAccessor)((CommandEncoderAccessor)RenderSystem.getDevice().createCommandEncoder()).sodium$getBackend()).sodium$setLastProgram(null);
        ChunkShaderOptions options = new ChunkShaderOptions(ChunkFogMode.SMOOTH, renderPass, vertexType);
        activeProgram = Vibrancy.getOrCompileOverlayProgram(options);
        activeProgram.bind();
        activeProgram.getInterface().setupState(renderPass, parameters, terrainSampler, uniformBuffer, sectionTimeInfoTexture);

        ChunkShaderInterface shader = this.activeProgram.getInterface();

        Iterator<ChunkRenderList> iterator = renderLists.iterator(renderPass.isTranslucent());

        while (iterator.hasNext()) {
            ChunkRenderList renderList = iterator.next();

            var region = renderList.getRegion();
            var storage = region.getStorage(renderPass);

            if (storage == null) {
                continue;
            }

            var resources = region.getResources();

            if (resources == null) {
                region.clearCachedBatchFor(renderPass);
                continue;
            }

            var batch = region.getCachedBatch(renderPass);

            if (!batch.isFilled) {
                fillCommandBuffer(batch, region, storage, renderList, camera, renderPass, useBlockFaceCulling, useIndexedTessellation);
            }

            if (batch.isEmpty()) {
                continue;
            }

            GlTessellation tessellation = ((DeviceResourcesExt) resources).vibrancy$getOverlayTessellation();

            if (tessellation == null) {
                tessellation = commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                        TessellationBinding.forVertexBuffer(resources.getGeometryBuffer(), vertexFormat.getShaderBindings()),
                        TessellationBinding.forElementBuffer(sharedIndexBuffer.getBufferObject()) // TODO
                });
                ((DeviceResourcesExt) resources).vibrancy$updateOverlayTessellation(commandList, tessellation);
            }

            setModelMatrixUniforms(shader, region, camera);
            executeDrawBatch(commandList, tessellation, batch);
        }

        activeProgram.getInterface().resetState();
        activeProgram.unbind();
        activeProgram = null;
    }
}
