package net.typho.vibrancy.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.typho.vibrancy.Vibrancy;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Shadow
    private static float red, green, blue;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V",
                    ordinal = 1
            )
    )
    private static void clearColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (Vibrancy.BETTER_SKY.getValue()) {
            switch (camera.getSubmersionType()) {
                case NONE -> {
                    Vec3d skyColor = world.getSkyColor(camera.getPos(), tickDelta);
                    red = (float) skyColor.x;
                    green = (float) skyColor.y;
                    blue = (float) skyColor.z;
                }
                case WATER -> {
                    float light = world.getLightLevel(camera.getBlockPos()) / 15f;
                    Vector3f color = new Vector3f(0.01f, 0.01f, 0.1f)
                            .lerp(new Vector3f(0.15f, 0.3f, 0.95f), light);
                    red *= color.x;
                    green *= color.y;
                    blue *= color.z;
                }
            }
        }
    }
}
