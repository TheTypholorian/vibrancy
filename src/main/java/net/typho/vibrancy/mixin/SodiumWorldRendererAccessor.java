package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(SodiumWorldRenderer.class)
public interface SodiumWorldRendererAccessor {
    @Accessor("renderSectionManager")
    RenderSectionManager vibrancy$getRenderSectionManager();
}
