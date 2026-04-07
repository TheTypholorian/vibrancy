package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(
            method = "isOnRenderThread",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void isOnRenderThread(CallbackInfoReturnable<Boolean> cir) {
        try {
            GL.getCapabilities();
            cir.setReturnValue(true);
        } catch (IllegalStateException ignored) {
        }
    }
}
