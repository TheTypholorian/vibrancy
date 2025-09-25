package net.typho.vibrancy.mixin.client;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.light.LightingProvider;
import net.typho.vibrancy.client.DynamicLightInfo;
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

                BlockState state = MinecraftClient.getInstance().world.getBlockState(pos);
                DynamicLightInfo info = DynamicLightInfo.get(state);

                if (info != null) {
                    VeilRenderSystem.renderer().getLightRenderer().addLight(new RaytracedPointBlockLight(pos).setFlicker(info.flicker().orElse(0f)).setBrightness(info.brightness().orElse(1f)).setColor(info.color().x, info.color().y, info.color().z).setRadius(info.radius().orElse((float) state.getLuminance())));
                }
            });
        }
    }
}
