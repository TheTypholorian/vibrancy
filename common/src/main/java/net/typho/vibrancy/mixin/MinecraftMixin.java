package net.typho.vibrancy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.typho.vibrancy.api.BlockLight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(
            method = "setLevel",
            at = @At("TAIL")
    )
    private void setLevel(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        BlockLight.Companion.getLIGHTS().values().forEach(BlockLight::free);
        BlockLight.Companion.getLIGHTS().clear();
    }
}
