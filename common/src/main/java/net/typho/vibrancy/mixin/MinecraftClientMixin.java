package net.typho.vibrancy.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.ReloadableResourceManagerImpl;
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

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow @Final private ReloadableResourceManagerImpl resourceManager;

    @Inject(
            method = "createInitScreens",
            at = @At("TAIL")
    )
    private void createInitScreens(List<Function<Runnable, Screen>> list, CallbackInfo ci) {
        if (!Vibrancy.SEEN_ALPHA_TEXT) {
            list.add(AlphaWarningScreen::new);
        }
    }

    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V",
            at = @At("HEAD")
    )
    private void disconnect(Screen disconnectionScreen, boolean transferring, CallbackInfo ci) {
        RaytracedPointEntityLight light = Vibrancy.ENTITY_LIGHTS.get(player);

        if (light != null) {
            light.free();
        }
    }

    @Inject(
            method = "enterReconfiguration",
            at = @At("HEAD")
    )
    private void enterReconfiguration(Screen reconfigurationScreen, CallbackInfo ci) {
        RaytracedPointEntityLight light = Vibrancy.ENTITY_LIGHTS.get(player);

        if (light != null) {
            light.free();
        }
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resource/ReloadableResourceManagerImpl;registerReloader(Lnet/minecraft/resource/ResourceReloader;)V",
                    ordinal = 0
            )
    )
    private void init(RunArgs args, CallbackInfo ci) {
        Vibrancy.registerReloadListeners(resourceManager);
    }

    @Inject(
            method = "setWorld",
            at = @At("TAIL")
    )
    private void afterClientWorldChange(ClientWorld world, CallbackInfo ci) {
        if (world != null) {
            Vibrancy.afterClientWorldChange(world);
        }
    }
}
