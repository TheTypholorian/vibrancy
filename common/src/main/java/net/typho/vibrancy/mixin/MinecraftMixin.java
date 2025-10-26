package net.typho.vibrancy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.typho.vibrancy.Vibrancy;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ReloadableResourceManager;registerReloadListener(Lnet/minecraft/server/packs/resources/PreparableReloadListener;)V",
                    ordinal = 0
            )
    )
    private void init(GameConfig gameConfig, CallbackInfo ci) {
        Vibrancy.registerReloadListeners(resourceManager);
    }

    @Inject(
            method = "setLevel",
            at = @At("TAIL")
    )
    private void afterClientLevelChange(ClientLevel level, CallbackInfo ci) {
        if (level != null) {
            Vibrancy.afterClientLevelChange(level);
        }
    }
}
