package net.typho.vibrancy.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.light.LightingProvider;
import net.typho.vibrancy.DynamicLightInfo;
import net.typho.vibrancy.RaytracedPointBlockLight;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightingProvider.class)
public class LightingProviderMixin {
    @Inject(
            method = "checkBlock",
            at = @At("TAIL")
    )
    private void checkBlock(BlockPos pos, CallbackInfo ci) {
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().world != null) {
                BlockState state = MinecraftClient.getInstance().world.getBlockState(pos);
                DynamicLightInfo info = DynamicLightInfo.get(state);

                if (info != null) {
                    info.addBlockLight(pos, state);
                } else {
                    RaytracedPointBlockLight light = Vibrancy.BLOCK_LIGHTS.get(pos);

                    if (light != null) {
                        light.free();
                    }
                }
            }
        });
    }
}
