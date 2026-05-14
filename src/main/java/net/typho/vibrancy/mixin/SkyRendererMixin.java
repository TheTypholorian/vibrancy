package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.math.Axis;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if <1.21.5 {
import net.minecraft.client.renderer.LevelRenderer;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(LevelRenderer.class)
public class SkyRendererMixin {
    @WrapOperation(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;",
                    ordinal = 3
            )
    )
    private Quaternionf renderSky(Axis instance, float f, Operation<Quaternionf> original) {
        return original.call(instance, f - 15);
    }
}
//? } else {
/*import net.minecraft.client.renderer.SkyRenderer;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
    @WrapOperation(
            method = "renderSunMoonAndStars",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;",
                    ordinal = 0
            )
    )
    private Quaternionf renderSky(Axis instance, float f, Operation<Quaternionf> original) {
        return original.call(instance, f - 15);
    }
}
*///? }