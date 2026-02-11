package net.typho.vibrancy.mixin;

import net.typho.big_shot_lib.api.shaders.mixins.ShaderOpcode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(value = ShaderOpcode.Builder.class, remap = false)
public class ShaderOpcodeBuilderMixin {
    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/ByteBuffer;asIntBuffer()Ljava/nio/IntBuffer;"
            )
    )
    private IntBuffer build(ByteBuffer instance) {
        return instance.flip().asIntBuffer();
    }
}
