package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.pipeline.MainTarget;
import net.typho.vibrancy.VibrancyDynamicBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MainTarget.class)
public class MainTargetMixin {
    @Inject(
            method = "createFrameBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/MainTarget;checkStatus()V"
            )
    )
    private void createFramebuffer(int width, int height, CallbackInfo ci) {
        VibrancyDynamicBuffers.init(width, height);
    }
}
