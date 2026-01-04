package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.typho.vibrancy.VibrancyDynamicBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public class RenderTargetMixin {
    @Inject(
            method = "createBuffers",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;checkStatus()V"
            )
    )
    private void createBuffers(int width, int height, boolean clearError, CallbackInfo ci) {
        if ((Object) this instanceof MainTarget) {
            VibrancyDynamicBuffers.resize(width, height);
        }
    }
}
