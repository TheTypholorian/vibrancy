package net.typho.vibrancy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.typho.vibrancy.AlphaWarningScreen;
import net.typho.vibrancy.RaytracedPointEntityLight;
import net.typho.vibrancy.Vibrancy;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    @Inject(
            method = "addInitialScreens",
            at = @At("TAIL")
    )
    private void addInitialScreens(List<Function<Runnable, Screen>> list, CallbackInfo ci) {
        if (!Vibrancy.SEEN_ALPHA_TEXT) {
            list.add(AlphaWarningScreen::new);
        }
    }

    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V",
            at = @At("HEAD")
    )
    private void disconnect(Screen disconnectionScreen, boolean transferring, CallbackInfo ci) {
        RaytracedPointEntityLight light = Vibrancy.ENTITY_LIGHTS.get(player);

        if (light != null) {
            light.free();
        }
    }

    @Inject(
            method = "clearClientLevel",
            at = @At("HEAD")
    )
    private void clearClientLevel(Screen nextScreen, CallbackInfo ci) {
        RaytracedPointEntityLight light = Vibrancy.ENTITY_LIGHTS.get(player);

        if (light != null) {
            light.free();
        }
    }

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
    private void afterClientLevelChange(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (level != null) {
            Vibrancy.afterClientLevelChange(level);
        }
    }
}
