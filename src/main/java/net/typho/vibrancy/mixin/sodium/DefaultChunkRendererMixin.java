package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBufferImpl;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSamplerImpl;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer;
import net.typho.vibrancy.RenderRegionExtension;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.VibrancyConfig;
import net.typho.vibrancy.block.impl.RayPointLightStorage;
import net.typho.vibrancy.block.impl.RayPointLightType;
import net.typho.vibrancy.util.ExtraAtlases;
import net.typho.vibrancy.util.LightBufferPacker;
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
    @SuppressWarnings("deprecation")
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
        if (VibrancyConfig.INSTANCE.getModEnabled() && VibrancyConfig.INSTANCE.getRayLightsEnabled()) {
            if (Vibrancy.lightManager.blockLights.get(RayPointLightType.INSTANCE) instanceof RayPointLightStorage lightStorage) {
                activeProgram = Vibrancy.raytracedPointRenderType.pipeline();

                // get atlases here because they might create render passes and vulkan doesn't like render pass inception
                GpuTextureView materialTex = ExtraAtlases.getMaterialOrThrow(TextureAtlas.LOCATION_BLOCKS).getTextureView();
                GpuTextureView transmissionTex = ExtraAtlases.getTransmissionOrThrow(TextureAtlas.LOCATION_BLOCKS).getTextureView();
                GpuBuffer configBuffer = VibrancyConfig.loadConfigBuffer();

                try (RenderPass pass = encoder.createRenderPass(() -> "Vibrancy Block Lights", renderPass.getTarget().getColorTextureView(), Optional.empty(), renderPass.getTarget().getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(this.activeProgram);
                    this.drawContext.setContext(pass, this.activeProgram);

                    if (!useIndexedTessellation && this.sharedIndexBuffer.getBufferObject() != null) {
                        pass.setIndexBuffer(this.sharedIndexBuffer.getBufferObject(), IndexType.INT);
                    }

                    pass.setUniform("Globals", RenderSystem.getGlobalSettingsUniform());
                    pass.setUniform("u_Globals", uniformData);
                    pass.setUniform("u_VibrancyConfig", configBuffer);
                    pass.setUniform("u_SectionTimeInfo", sectionTimeInfo);
                    pass.bindTexture("u_BlockTex", renderPass.getAtlas(), terrainSampler);
                    pass.bindTexture("u_MaterialTex", materialTex, terrainSampler);
                    pass.bindTexture("u_TransmissionTex", transmissionTex, terrainSampler);

                    renderLists.iterator(renderPass.isTranslucent()).forEachRemaining(renderList -> {
                        RenderRegion region = renderList.getRegion();
                        SectionRenderDataStorage storage = region.getStorage(renderPass);

                        if (storage != null) {
                            RenderRegionExtension ext = (RenderRegionExtension) region;

                            if (lightStorage.getDirty() || Vibrancy.lightManager.dirtySections.stream().anyMatch(section -> (section.getFirst().getX() >> 3) == region.getX() && (section.getFirst().getY() >> 2) == region.getY() && (section.getFirst().getZ() >> 3) == region.getZ()) || !ext.getVibrancy$initialized()) {
                                LightBufferPacker.pack(region, lightStorage.getMap().values(), Vibrancy.lightManager, ext);
                                ext.setVibrancy$initialized(true);
                            }

                            GpuBuffer lightBuffer = ext.getVibrancy$lightBuffer();
                            GpuBuffer shadowBuffer = ext.getVibrancy$shadowBuffer();
                            GpuBuffer gridBuffer = ext.getVibrancy$gridBuffer();

                            // TODO shadowBuffer null
                            if (lightBuffer != null && gridBuffer != null) {
                                MultiDrawBatch batch = region.getCachedBatch(renderPass);

                                if (!batch.isEmpty()) {
                                    if (useIndexedTessellation) {
                                        pass.setIndexBuffer(region.getResources().getIndexBuffer(), IndexType.INT);
                                    }

                                    pass.setUniform("u_Lights", lightBuffer);

                                    if (shadowBuffer == null) {
                                        pass.setUniform("u_Shadows", RenderRegionExtension.getEmptyShadowBuffer());
                                    } else {
                                        pass.setUniform("u_Shadows", shadowBuffer);
                                    }

                                    pass.setUniform("u_Grids", gridBuffer);
                                    pass.setVertexBuffer(0, region.getResources().getGeometryBuffer().slice());
                                    this.drawContext.updateData(region, camera);
                                    batch.draw(this.drawContext);
                                }
                            }
                        }
                    });
                }

                this.drawContext.endDraw();
                activeProgram = null;
            }
        }
    }
}
