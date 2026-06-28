package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBufferImpl;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSamplerImpl;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.Minecraft;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalDouble;

@Mixin(DefaultChunkRenderer.class)
public abstract class DefaultChunkRendererMixin extends ShaderChunkRenderer {
    @Shadow
    @Final
    private DrawContext drawContext;

    @Shadow
    @Final
    private SharedQuadIndexBuffer sharedIndexBuffer;

    public DefaultChunkRendererMixin(ChunkVertexType vertexType) {
        super(vertexType);
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void render(
            ChunkRenderMatrices matrices,
            ChunkRenderListIterable renderLists,
            TerrainRenderPass renderPass,
            CameraTransform camera,
            FogParameters parameters,
            boolean indexedRenderingEnabled,
            GpuSamplerImpl terrainSampler,
            GpuBufferImpl uniformData,
            GpuBufferImpl sectionTimeInfo,
            CallbackInfo ci,
            @Local CommandEncoder encoder,
            @Local(ordinal = 2) boolean useIndexedTessellation
    ) {
        activeProgram = Vibrancy.blockLightRenderPipeline.pipeline();

        try (RenderPass pass = encoder.createRenderPass(() -> "Vibrancy Block Lights", renderPass.getTarget().getColorTextureView(), Optional.empty(), renderPass.getTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(this.activeProgram);
            this.drawContext.setContext(pass, this.activeProgram);

            if (!useIndexedTessellation && this.sharedIndexBuffer.getBufferObject() != null) {
                pass.setIndexBuffer(this.sharedIndexBuffer.getBufferObject(), IndexType.INT);
            }

            pass.setUniform("u_Globals", uniformData);
            pass.setUniform("u_SectionTimeInfo", sectionTimeInfo);
            pass.bindTexture("u_LightTex", Minecraft.getInstance().gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.bindTexture("u_BlockTex", renderPass.getAtlas(), terrainSampler);

            renderLists.iterator(renderPass.isTranslucent()).forEachRemaining(renderList -> {
                RenderRegion region = renderList.getRegion();
                SectionRenderDataStorage storage = region.getStorage(renderPass);

                if (storage != null) {
                    MultiDrawBatch batch = region.getCachedBatch(renderPass);

                    if (!batch.isEmpty()) {
                        if (useIndexedTessellation) {
                            pass.setIndexBuffer(region.getResources().getIndexBuffer(), IndexType.INT);
                        }

                        pass.setVertexBuffer(0, region.getResources().getGeometryBuffer().slice());
                        this.drawContext.updateData(region, camera);
                        batch.draw(this.drawContext);
                    }
                }
            });
        }
    }
}
