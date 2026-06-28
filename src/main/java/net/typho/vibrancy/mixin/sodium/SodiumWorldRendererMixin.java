package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.textures.GpuSamplerImpl;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.typho.vibrancy.TerrainLightTexture;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {
    @WrapMethod(
            method = "renderLayer"
    )
    private void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z, FogParameters fogParameters, GpuSamplerImpl terrainSampler, Operation<Void> original) {
        TerrainLightTexture.inUse.set(true);

        original.call(matrices, pass, x, y, z, fogParameters, terrainSampler);

        TerrainLightTexture.inUse.remove();
    }
}
