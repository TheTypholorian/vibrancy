package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public class WindowMixin {
    @Redirect(
            method = "defaultErrorCallback",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V"
            )
    )
    private void assertOnRenderThread() {
    }
}
