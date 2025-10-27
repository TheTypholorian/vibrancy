package net.typho.vibrancy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.light.EntityPointLight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
    public ClientPacketListenerMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    @Inject(
            method = "handleLogin",
            at = @At("TAIL")
    )
    private void onGameJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        Vibrancy.ENTITY_LIGHTS.computeIfAbsent(minecraft.player, EntityPointLight::new);
    }

    @Inject(
            method = "handleRespawn",
            at = @At("TAIL")
    )
    private void onPlayerRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        Vibrancy.ENTITY_LIGHTS.computeIfAbsent(minecraft.player, EntityPointLight::new);
    }
}
