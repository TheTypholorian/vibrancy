package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.GameRenderer;
import net.typho.vibrancy.TerrainLightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LightTexture;tick()V"
            )
    )
    private void tick(CallbackInfo ci) {
        TerrainLightTexture.INSTANCE.tick();
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LightTexture;updateLightTexture(F)V"
            )
    )
    private void renderLevel(CallbackInfo ci, @Local(ordinal = 0) float tickDelta) {
        TerrainLightTexture.INSTANCE.updateLightTexture(tickDelta);
    }
}
