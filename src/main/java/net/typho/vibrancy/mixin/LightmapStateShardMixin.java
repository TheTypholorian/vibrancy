package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.typho.vibrancy.BlockLightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderStateShard.LightmapStateShard.class)
public class LightmapStateShardMixin {
    @WrapOperation(
            //? fabric {
            method = {
                    "method_23551",
                    "method_23552"
            },
            //? } neoforge {
            /*method = {
                    "lambda$new$0",
                    "lambda$new$1"
            },
            *///? }
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;lightTexture()Lnet/minecraft/client/renderer/LightTexture;"
            )
    )
    private static LightTexture turnOnLightLayer(GameRenderer instance, Operation<LightTexture> original) {
        if (BlockLightTexture.isInUse()) {
            return BlockLightTexture.INSTANCE;
        } else {
            return original.call(instance);
        }
    }
}
