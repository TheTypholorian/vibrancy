package net.typho.vibrancy.mixin.render;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;

@Mixin(GlCommandEncoder.class)
public interface GlCommandEncoderAccessor {
    @Invoker("trySetup")
    boolean vibrancy$trySetup(GlRenderPass pass, Collection<String> dynamicUniforms);

    @Accessor("lastProgram")
    GlProgram vibrancy$lastProgram();
}
