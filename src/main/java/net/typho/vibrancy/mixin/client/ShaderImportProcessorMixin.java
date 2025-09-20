package net.typho.vibrancy.mixin.client;

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
                    target = "Lnet/minecraft/util/Identifier;of(Ljava/lang/String;)Lnet/minecraft/util/Identifier;"
            )
    )
    private String modify(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }

        return s;
    }
}

