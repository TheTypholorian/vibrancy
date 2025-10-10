package net.typho.vibrancy.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.typho.vibrancy.RaytracedPointEntityLight;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler {
    protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(
            method = "onGameJoin",
            at = @At("TAIL")
    )
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        System.out.println(client.player);
        Vibrancy.ENTITY_LIGHTS.computeIfAbsent(client.player, RaytracedPointEntityLight::new);
    }

    @Inject(
            method = "onPlayerRespawn",
            at = @At("TAIL")
    )
    private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        System.out.println(client.player);
        Vibrancy.ENTITY_LIGHTS.computeIfAbsent(client.player, RaytracedPointEntityLight::new);
    }
}
