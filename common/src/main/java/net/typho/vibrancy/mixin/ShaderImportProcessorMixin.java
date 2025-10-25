package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.shader.processor.ShaderImportProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ShaderImportProcessor.class, remap = false)
public class ShaderImportProcessorMixin {
    @ModifyArg(
            method = "modify",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;parse(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"
            )
    )
    private String modify(String s) {
        s = s.trim();

        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }

        return s;
    }
}
