package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.typho.vibrancy.TerrainLightTexture;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderSectionManager.class)
public class RenderSectionManagerMixin {
    @WrapMethod(
            method = "renderLayer"
    )
    private void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z, Operation<Void> original) {
        TerrainLightTexture.inUse.set(true);

        original.call(matrices, pass, x, y, z);

        TerrainLightTexture.inUse.remove();
    }
}
