package net.typho.vibrancy.mixin.client;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.BlockLightStorage;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;
import net.typho.vibrancy.client.RaytracedPointLight;
import net.typho.vibrancy.client.VibrancyClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(ChunkBlockLightProvider.class)
public class ChunkBlockLightProviderMixin {
    @Unique
    private Map<BlockPos, RaytracedPointLight> blockLights;

    @Inject(
            method = "<init>(Lnet/minecraft/world/chunk/ChunkProvider;Lnet/minecraft/world/chunk/light/BlockLightStorage;)V",
            at = @At("TAIL")
    )
    private void init(ChunkProvider chunkProvider, BlockLightStorage blockLightStorage, CallbackInfo ci) {
        blockLights = new LinkedHashMap<>();
    }

    @Inject(
            method = "propagateLight",
            at = @At("HEAD")
    )
    private void clearLight(ChunkPos chunkPos, CallbackInfo ci) {
        VeilRenderSystem.renderer().getLightRenderer().getLights(VibrancyClient.RAY_POINT_LIGHT.get()).removeAll(blockLights.values());
        blockLights.clear();
    }

    @Inject(
            method = "method_51532",
            at = @At("TAIL")
    )
    private void propagateLight(BlockPos pos, BlockState state, CallbackInfo ci) {
        RaytracedPointLight light = (RaytracedPointLight) new RaytracedPointLight().setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).setRadius(state.getLuminance());
        VeilRenderSystem.renderer().getLightRenderer().getLights(VibrancyClient.RAY_POINT_LIGHT.get()).add(light);
        blockLights.put(pos, light);
    }
}
