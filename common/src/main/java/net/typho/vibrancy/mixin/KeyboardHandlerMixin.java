package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.network.chat.Component;
import net.typho.vibrancy.util.VibrancyDebugKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow
    protected abstract void debugFeedbackComponent(Component message);

    @Inject(
            method = "keyPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z"
            ),
            cancellable = true
    )
    private void keyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        // TODO replace with keymapping
        if (InputConstants.isKeyDown(windowPointer, VibrancyDebugKeys.KEY) && key != VibrancyDebugKeys.KEY) {
            if (action == 0) {
                VibrancyDebugKeys.action(key, this::debugFeedbackComponent);
            }

            ci.cancel();
        }
    }
}
