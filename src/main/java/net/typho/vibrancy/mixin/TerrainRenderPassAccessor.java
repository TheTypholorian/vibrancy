package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TerrainRenderPass.class)
public interface TerrainRenderPassAccessor {
    @Accessor("renderType")
    ChunkSectionLayer vibrancy$getRenderType();
}
