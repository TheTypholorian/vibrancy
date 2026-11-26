package net.typho.vibrancy.mixin.render;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlRenderPass.class)
public interface GlRenderPassAccessor {
    @Accessor("encoder")
    GlCommandEncoder vibrancy$encoder();
}
