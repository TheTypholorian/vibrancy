package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.typho.vibrancy.VibrancyDynamicBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlStateManager.class, remap = false)
public class GlStateManagerMixin {
    @Inject(
            method = "_enableBlend",
            at = @At("TAIL")
    )
    private static void enableBlend(CallbackInfo ci) {
        if (GlStateManager.getBoundFramebuffer() == Minecraft.getInstance().getMainRenderTarget().frameBufferId) {
            VibrancyDynamicBuffers.initState();
        }
    }
}
