package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
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
            at = @At("HEAD"),
            cancellable = true
    )
    private static void clearColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (Vibrancy.BETTER_FOG.getValue()) {
            float red1 = red, green1 = green, blue1 = blue;

            switch (camera.getSubmersionType()) {
                case NONE -> {
                    Vec3d skyColor = world.getSkyColor(camera.getPos(), tickDelta);
                    red1 = (float) skyColor.x;
                    green1 = (float) skyColor.y;
                    blue1 = (float) skyColor.z;
                }
                case WATER -> {
                    float light = world.getLightLevel(camera.getBlockPos()) / 15f;
                    int water = world.getBiome(camera.getBlockPos()).value().getWaterColor();
                    Vector3f color = new Vector3f(0, 0, 0.01f)
                            .lerp(new Vector3f(
                                    (water >> 16) / 255f,
                                    ((water >> 8) & 0xFF) / 255f,
                                    (water & 0xFF) / 255f
                            ), light);
                    red1 = color.x;
                    green1 = color.y;
                    blue1 = color.z;
                }
            }

            red = MathHelper.lerp(tickDelta / 5, red, red1);
            green = MathHelper.lerp(tickDelta / 5, green, green1);
            blue = MathHelper.lerp(tickDelta / 5, blue, blue1);

            RenderSystem.clearColor(red, green, blue, 1);
            ci.cancel();
        }
    }
}
