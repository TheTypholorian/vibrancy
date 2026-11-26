package net.typho.vibrancy.mixin.render;

import net.caffeinemc.mods.sodium.client.gl.array.GlVertexArray;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlVertexArrayTessellation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlVertexArrayTessellation.class)
public interface GlVertexArrayTessellationAccessor {
    @Accessor("array")
    GlVertexArray vibrancy$array();
}
