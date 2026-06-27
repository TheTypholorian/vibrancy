package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import net.typho.vibrancy.TerrainLightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @WrapOperation(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LightTexture;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F",
                    ordinal = 0
            )
    )
    private float updateLightTexture1(DimensionType dimension, int x, Operation<Float> original) {
        if ((Object) this instanceof TerrainLightTexture blockLight) {
            return blockLight.getSkyBrightness(dimension, x);
        }

        return original.call(dimension, x);
    }

    @WrapOperation(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LightTexture;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F",
                    ordinal = 1
            )
    )
    private float updateLightTexture2(DimensionType dimension, int x, Operation<Float> original) {
        if ((Object) this instanceof TerrainLightTexture blockLight) {
            return blockLight.getBlockBrightness(dimension, x);
        }

        return original.call(dimension, x);
    }
}
