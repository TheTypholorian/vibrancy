package net.typho.vibrancy.mixin.sodium;

//? fabric {
/*import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRendering;
*///? } neoforge {
import net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
//? }

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas;
import net.typho.big_shot_lib.api.util.WrapperUtil;
import net.typho.vibrancy.util.SectionMeshCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRendererImpl.class)
public class FluidRendererImplMixin {
    //? fabric {
    /*@Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/fabric/render/FluidRendererImpl$DefaultRenderContext;setUp(Lnet/caffeinemc/mods/sodium/client/model/color/ColorProviderRegistry;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;Lnet/fabricmc/fabric/api/client/render/fluid/v1/FluidRenderHandler;Z)V"
            )
    )
    private void render(
            LevelSlice level,
            BlockState blockState,
            FluidState fluidState,
            BlockPos blockPos,
            BlockPos offset,
            TranslucentGeometryCollector collector,
            ChunkBuildBuffers buffers,
            CallbackInfo ci,
            @Local ChunkModelBuilder meshBuilder,
            @Local Material material
    ) {
        SectionMeshCache cache = ((SectionMeshCache.Holder) buffers).getVibrancy$sectionMeshCache();

        if (cache == null) {
            ((SectionMeshCache.ConsumerExtension) meshBuilder).setVibrancy$sectionMeshConsumer(null);
        } else {
            ((SectionMeshCache.ConsumerExtension) meshBuilder).setVibrancy$sectionMeshConsumer(cache.createVertexConsumer(blockPos, material.isTranslucent(), NeoAtlas.Companion.getBlocks(), -SectionPos.sectionRelative(blockPos.getX()), -SectionPos.sectionRelative(blockPos.getY()), -SectionPos.sectionRelative(blockPos.getZ())));
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/client/render/fluid/v1/FluidRendering;render(Lnet/fabricmc/fabric/api/client/render/fluid/v1/FluidRenderHandler;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/fabricmc/fabric/api/client/render/fluid/v1/FluidRendering$DefaultRenderer;)V"
            )
    )
    private void render(
            FluidRenderHandler handler,
            BlockAndTintGetter world,
            BlockPos pos,
            VertexConsumer vertexConsumer,
            BlockState blockState,
            FluidState fluidState,
            FluidRendering.DefaultRenderer defaultRenderer,
            Operation<Void> original,
            @Local ChunkModelBuilder meshBuilder
    ) {
        var consumer = ((SectionMeshCache.ConsumerExtension) meshBuilder).getVibrancy$sectionMeshConsumer();

        if (consumer == null) {
            original.call(handler, world, pos, vertexConsumer, blockState, fluidState, defaultRenderer);
        } else {
            original.call(handler, world, pos, VertexMultiConsumer.create(vertexConsumer, WrapperUtil.Companion.getINSTANCE().unwrap(consumer)), blockState, fluidState, defaultRenderer);
        }
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void render(
            LevelSlice level,
            BlockState blockState,
            FluidState fluidState,
            BlockPos blockPos,
            BlockPos offset,
            TranslucentGeometryCollector collector,
            ChunkBuildBuffers buffers,
            CallbackInfo ci,
            @Local ChunkModelBuilder meshBuilder
    ) {
        var consumer = ((SectionMeshCache.ConsumerExtension) meshBuilder).getVibrancy$sectionMeshConsumer();

        if (consumer != null) {
            consumer.flush();
        }

        ((SectionMeshCache.ConsumerExtension) meshBuilder).setVibrancy$sectionMeshConsumer(null);
    }
    *///? } neoforge {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/neoforge/render/FluidRendererImpl$DefaultRenderContext;setUp(Lnet/caffeinemc/mods/sodium/client/model/color/ColorProviderRegistry;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;)V"
            )
    )
    private void render(
            LevelSlice level,
            BlockState blockState,
            FluidState fluidState,
            BlockPos blockPos,
            BlockPos offset,
            TranslucentGeometryCollector collector,
            ChunkBuildBuffers buffers,
            CallbackInfo ci,
            @Local ChunkModelBuilder meshBuilder,
            @Local Material material
    ) {
        SectionMeshCache cache = ((SectionMeshCache.Holder) buffers).getVibrancy$sectionMeshCache();

        if (cache == null) {
            ((SectionMeshCache.ConsumerExtension) meshBuilder).setVibrancy$sectionMeshConsumer(null);
        } else {
            ((SectionMeshCache.ConsumerExtension) meshBuilder).setVibrancy$sectionMeshConsumer(cache.createVertexConsumer(blockPos, material.isTranslucent(), NeoAtlas.Companion.getBlocks(), -SectionPos.sectionRelative(blockPos.getX()), -SectionPos.sectionRelative(blockPos.getY()), -SectionPos.sectionRelative(blockPos.getZ())));
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;renderFluid(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean render(
            IClientFluidTypeExtensions instance,
            FluidState fluidState,
            BlockAndTintGetter getter,
            BlockPos pos,
            VertexConsumer vertexConsumer,
            BlockState blockState,
            Operation<Boolean> original,
            @Local ChunkModelBuilder meshBuilder
    ) {
        var consumer = ((SectionMeshCache.ConsumerExtension) meshBuilder).getVibrancy$sectionMeshConsumer();

        if (consumer == null) {
            original.call(instance, fluidState, getter, pos, vertexConsumer, blockState);
        } else {
            original.call(instance, fluidState, getter, pos, VertexMultiConsumer.create(vertexConsumer, WrapperUtil.Companion.getINSTANCE().unwrap(consumer)), blockState);
        }
        return false;
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void render(
            LevelSlice level,
            BlockState blockState,
            FluidState fluidState,
            BlockPos blockPos,
            BlockPos offset,
            TranslucentGeometryCollector collector,
            ChunkBuildBuffers buffers,
            CallbackInfo ci,
            @Local ChunkModelBuilder meshBuilder
    ) {
        var consumer = ((SectionMeshCache.ConsumerExtension) meshBuilder).getVibrancy$sectionMeshConsumer();

        if (consumer != null) {
            consumer.flush();
        }

        ((SectionMeshCache.ConsumerExtension) meshBuilder).setVibrancy$sectionMeshConsumer(null);
    }
    //? }
}
