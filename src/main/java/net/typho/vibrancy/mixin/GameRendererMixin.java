package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /*
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
     */
}
