package net.typho.vibrancy.mixin.client;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.light.LightingProvider;
import net.typho.vibrancy.client.RaytracedPointBlockLight;
import net.typho.vibrancy.client.RaytracedPointLight;
import net.typho.vibrancy.client.VibrancyClient;
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
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            MinecraftClient.getInstance().execute(() -> {
                List<RaytracedPointLight> lights = VeilRenderSystem.renderer().getLightRenderer().getLights(VibrancyClient.RAY_POINT_LIGHT.get());

                for (RaytracedPointLight light : lights) {
                    if (light instanceof RaytracedPointBlockLight block && block.blockPos.equals(pos)) {
                        VeilRenderSystem.renderer().getLightRenderer().removeLight(light);
                    }
                }

                VeilRenderSystem.renderer().getLightRenderer().addLight(new RaytracedPointBlockLight(pos).setBrightness(0.5f).setColor(0.97f, 0.92f, 0.83f).setRadius(MinecraftClient.getInstance().world.getBlockState(pos).getLuminance()));
            });
        }
    }
}
