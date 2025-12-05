package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.shader.processor.ShaderImportProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShaderImportProcessor.class)
public class ShaderImportProcessorMixin {
    @ModifyArg(
            method = "modify",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;parse(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"
            )
    )
    private String modify(String importId) {
        if ((importId.startsWith("\"") && importId.endsWith("\"")) || (importId.startsWith("<") && importId.endsWith(">"))) {
            importId = importId.substring(1, importId.length() - 1);
        }

        return importId;
    }
}
