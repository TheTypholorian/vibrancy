package net.typho.vibrancy.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSectionManager.class)
public interface RenderSectionManagerAccessor {
    @Accessor("renderTree")
    SectionTree vibrancy$getRenderTree();
}
