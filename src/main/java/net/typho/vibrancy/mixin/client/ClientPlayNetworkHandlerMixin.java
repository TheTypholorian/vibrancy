package net.typho.vibrancy.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.world.chunk.WorldChunk;
import net.typho.vibrancy.client.DynamicLightInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientWorld world;

    @Inject(
            method = "readLightData",
            at = @At("TAIL")
    )
    private void readLightData(int x, int z, LightData data, CallbackInfo ci) {
        WorldChunk chunk = world.getChunk(x, z);

        if (chunk != null) {
            chunk.forEachBlockMatchingPredicate(
                    state -> DynamicLightInfo.get(state) != null,
                    (pos, state) -> MinecraftClient.getInstance().execute(() -> DynamicLightInfo.get(state).addLight(pos, state, false))
            );
        }
    }
}
