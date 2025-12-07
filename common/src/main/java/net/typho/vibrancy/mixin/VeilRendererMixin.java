package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.VeilRenderer;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = VeilRenderer.class, remap = false)
public class VeilRendererMixin {
    @Inject(
            method = "addDebugInfo",
            at = @At("TAIL")
    )
    private void addDebugInfo(Consumer<String> consumer, CallbackInfo ci) {
        Vibrancy.INSTANCE.addDebugInfo(consumer);
    }
}
