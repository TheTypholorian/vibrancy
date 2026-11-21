package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderRegion.class)
public interface RenderRegionAccessor {
    @Accessor("sections")
    RenderSection[] vibrancy$sections();
}
