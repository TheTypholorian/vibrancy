package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import net.minecraft.util.Identifier;
import net.typho.vibrancy.RaytracedPointBlockLightRenderer;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = LightRenderer.class, remap = false)
public class LightRendererMixin {
    @Shadow
    @Final
    private static Identifier BUFFER_ID;

    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private void render(CallbackInfo ci) {
        if (VeilRenderSystem.renderer().enableBuffers(BUFFER_ID, DynamicBufferType.ALBEDO, DynamicBufferType.NORMAL)) {
            RaytracedPointBlockLightRenderer.INSTANCE.render();
        }
    }

    @Inject(
            method = "addDebugInfo",
            at = @At("TAIL")
    )
    private void addDebugInfo(Consumer<String> consumer, CallbackInfo ci) {
        consumer.accept("Dynamic Lights: " + RaytracedPointBlockLightRenderer.INSTANCE.numVisible + " / " + RaytracedPointBlockLightRenderer.INSTANCE.lights.size());

        if (Vibrancy.MAX_RAYTRACED_LIGHTS.getValue() > 100) {
            consumer.accept("Raytraced: " + RaytracedPointBlockLightRenderer.INSTANCE.numRaytraced);
        } else {
            consumer.accept("Raytraced: " + RaytracedPointBlockLightRenderer.INSTANCE.numRaytraced + " / " + Vibrancy.MAX_RAYTRACED_LIGHTS.getValue());
        }
    }
}
