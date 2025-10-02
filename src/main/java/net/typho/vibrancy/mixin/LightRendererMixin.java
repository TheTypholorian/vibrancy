package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.light.renderer.LightRenderer;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = LightRenderer.class, remap = false)
public class LightRendererMixin {
    @Inject(
            method = "addDebugInfo",
            at = @At("TAIL")
    )
    private void addDebugInfo(Consumer<String> consumer, CallbackInfo ci) {
        consumer.accept("Dynamic Lights: " + Vibrancy.NUM_VISIBLE_LIGHTS + " / " + (Vibrancy.BLOCK_LIGHTS.size() + Vibrancy.ENTITY_LIGHTS.size()));

        if (Vibrancy.MAX_RAYTRACED_LIGHTS.getValue() > 100) {
            consumer.accept("Raytraced: " + Vibrancy.NUM_RAYTRACED_LIGHTS);
        } else {
            consumer.accept("Raytraced: " + Vibrancy.NUM_RAYTRACED_LIGHTS + " / " + Vibrancy.MAX_RAYTRACED_LIGHTS.getValue());
        }

        consumer.accept("Async tasks: " + Vibrancy.NUM_LIGHT_TASKS);
    }
}
