package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VeilRenderSystem.class, remap = false)
public class VeilRenderSystemMixin {
    @WrapOperation(
            method = "blit",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo;bind(Z)V"
            )
    )
    private static void blit(AdvancedFbo fbo, boolean setViewport, Operation<Void> original) {
        original.call(fbo, false);
    }
}
