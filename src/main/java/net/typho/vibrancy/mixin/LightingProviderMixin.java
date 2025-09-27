package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.light.LightingProvider;
import net.typho.vibrancy.DynamicLightInfo;
import net.typho.vibrancy.RaytracedPointBlockLight;
import net.typho.vibrancy.RaytracedPointLight;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

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
                    info.addLight(pos, state, true);
                } else {
                    List<RaytracedPointLight> lights = VeilRenderSystem.renderer().getLightRenderer().getLights(Vibrancy.RAY_POINT_LIGHT.get());

                    for (RaytracedPointLight light : lights) {
                        if (light instanceof RaytracedPointBlockLight block && block.blockPos.equals(pos)) {
                            VeilRenderSystem.renderer().getLightRenderer().removeLight(light);
                        }
                    }
                }
            }
        });
    }
}
