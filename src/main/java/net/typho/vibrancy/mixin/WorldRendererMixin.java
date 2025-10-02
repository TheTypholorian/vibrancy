package net.typho.vibrancy.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.WorldRenderer;
import net.typho.vibrancy.Vibrancy;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldRenderer.class, priority = 2000)
public class WorldRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(
            method = "renderSky",
            at = @At("HEAD"),
            cancellable = true
    )
    private void renderSky(Matrix4f viewMat, Matrix4f projMat, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        if (Vibrancy.BETTER_SKY.getValue() && client.world.getDimensionEffects().getSkyType() == DimensionEffects.SkyType.NORMAL) {
            fogCallback.run();
            Vibrancy.renderSky(viewMat, projMat, tickDelta, camera, thickFog, client, (WorldRenderer) (Object) this);
            ci.cancel();
        }
    }
}
