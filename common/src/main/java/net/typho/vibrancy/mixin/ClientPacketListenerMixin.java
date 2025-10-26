package net.typho.vibrancy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.typho.vibrancy.RaytracedPointEntityLight;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "handleLogin",
            at = @At("TAIL")
    )
    private void onGameJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        Vibrancy.ENTITY_LIGHTS.computeIfAbsent(minecraft.player, RaytracedPointEntityLight::new);
    }

    @Inject(
            method = "handleRespawn",
            at = @At("TAIL")
    )
    private void onPlayerRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        Vibrancy.ENTITY_LIGHTS.computeIfAbsent(minecraft.player, RaytracedPointEntityLight::new);
    }
}
