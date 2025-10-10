package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.typho.vibrancy.Vibrancy;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Shadow
    private static float red = 1, green = 1, blue = 1;

    @Shadow
    @Nullable
    private static BackgroundRenderer.StatusEffectFogModifier getFogModifier(Entity entity, float tickDelta) {
        return null;
    }

    @Unique
    private static float light = 1;

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void betterFog(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (Vibrancy.BETTER_FOG.getValue()) {
            float red1 = red, green1 = green, blue1 = blue;

            switch (camera.getSubmersionType()) {
                case NONE, POWDER_SNOW, LAVA -> {
                    return;
                }
                case WATER -> {
                    float light = world.getLightLevel(camera.getBlockPos()) / 15f;
                    BackgroundRendererMixin.light = MathHelper.lerp(tickDelta / 10, light, BackgroundRendererMixin.light);
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

            BackgroundRenderer.StatusEffectFogModifier modifier = getFogModifier(camera.getFocusedEntity(), tickDelta);

            if (modifier != null) {
                LivingEntity entity = (LivingEntity) camera.getFocusedEntity();
                float f = modifier.applyColorModifier(entity, entity.getStatusEffect(modifier.getStatusEffect()), 1, tickDelta);
                red1 *= f;
                green1 *= f;
                blue1 *= f;
            }

            red = MathHelper.lerp(tickDelta / 10, red, red1);
            green = MathHelper.lerp(tickDelta / 10, green, green1);
            blue = MathHelper.lerp(tickDelta / 10, blue, blue1);

            RenderSystem.clearColor(red, green, blue, 1);
            ci.cancel();
        }
    }

    @ModifyConstant(
            method = "applyFog",
            constant = @Constant(floatValue = 96)
    )
    private static float waterFogEnd(float constant, @Local(argsOnly = true) Camera camera) {
        return constant * (light / 2 + 0.5f);
    }
}
