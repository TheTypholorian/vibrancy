package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSectionManager.class)
public interface RenderSectionManagerAccessor {
    @Accessor("sectionCache")
    ClonedChunkSectionCache vibrancy$sectionCache();
}
