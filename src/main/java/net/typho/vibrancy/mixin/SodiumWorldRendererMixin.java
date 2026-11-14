package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.VibrancyRenderPasses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {
    @WrapOperation(
            method = "drawChunkLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/SodiumWorldRenderer;renderLayer(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;DDDLnet/caffeinemc/mods/sodium/client/util/FogParameters;Lcom/mojang/blaze3d/textures/GpuSampler;)V"
            )
    )
    private void drawChunkLayer(
            SodiumWorldRenderer instance,
            CommandList commandList,
            ChunkRenderMatrices matrices,
            TerrainRenderPass pass,
            double x,
            double y,
            double z,
            FogParameters fogParameters,
            GpuSampler terrainSampler,
            Operation<Void> original
    ) {
        original.call(instance, commandList, matrices, pass, x, y, z, fogParameters, terrainSampler);
        original.call(instance, commandList, matrices, VibrancyRenderPasses.MAP.get(pass), x, y, z, fogParameters, terrainSampler);
    }
}
