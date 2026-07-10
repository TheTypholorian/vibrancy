package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBufferImpl;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
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
import net.minecraft.client.renderer.RenderType;
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer;
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjectName;
import net.typho.vibrancy.TerrainOverlayContext;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.VibrancyConfig;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
            GpuBufferSlice uniformData,
            GpuBufferImpl sectionTimeInfo,
            CallbackInfo ci,
            @Local CommandEncoder encoder,
            @Local(ordinal = 2) boolean useIndexedTessellation
    ) {
        if (VibrancyConfig.INSTANCE.getModEnabled()) {
            Vibrancy.lightManager.render(new TerrainOverlayContext() {
                @Override
                @NotNull
                public ChunkRenderMatrices getMatrices() {
                    return matrices;
                }

                @Override
                @NotNull
                public TerrainRenderPass getTerrainType() {
                    return renderPass;
                }

                @Override
                @NotNull
                public CameraTransform getCamera() {
                    return camera;
                }

                @Override
                @NotNull
                public FogParameters getFog() {
                    return parameters;
                }

                @Override
                @NotNull
                public GpuSamplerImpl getTerrainSampler() {
                    return terrainSampler;
                }

                @Override
                @NotNull
                public GpuBufferSlice getGlobals() {
                    return uniformData;
                }

                @Override
                @NotNull
                public GpuBuffer getSectionTimeInfo() {
                    return sectionTimeInfo;
                }

                @Override
                @NotNull
                public CommandEncoder getEncoder() {
                    return encoder;
                }

                @Override
                public void pass(@NotNull GpuObjectName name, @NotNull RenderType renderType, @NotNull Function<RenderPass, BiConsumer<ChunkRenderList, Runnable>> out) {
                    activeProgram = renderType.pipeline();

                    try (RenderPass pass = encoder.createRenderPass(
                            name,
                            renderPass.getTarget().getColorTextureView(),
                            Optional.empty(),
                            renderPass.getTarget().getDepthTextureView(),
                            OptionalDouble.empty()
                    )) {
                        pass.setPipeline(activeProgram);
                        drawContext.setContext(pass, activeProgram);

                        if (!useIndexedTessellation && sharedIndexBuffer.getBufferObject() != null) {
                            pass.setIndexBuffer(sharedIndexBuffer.getBufferObject(), IndexType.INT);
                        }

                        var out1 = out.apply(pass);

                        renderLists.iterator(renderPass.isTranslucent()).forEachRemaining(renderList -> {
                            RenderRegion region = renderList.getRegion();
                            SectionRenderDataStorage storage = region.getStorage(renderPass);

                            if (storage != null) {
                                MultiDrawBatch batch = region.getCachedBatch(renderPass);

                                if (!batch.isEmpty()) {
                                    if (useIndexedTessellation) {
                                        pass.setIndexBuffer(region.getResources().getIndexBuffer(), IndexType.INT);
                                    }

                                    drawContext.updateData(region, camera);
                                    pass.setVertexBuffer(0, region.getResources().getGeometryBuffer().slice());

                                    out1.accept(renderList, () -> batch.draw(drawContext));
                                }
                            }
                        });
                    }

                    drawContext.endDraw();
                    activeProgram = null;
                }
            });
        }
    }
}
