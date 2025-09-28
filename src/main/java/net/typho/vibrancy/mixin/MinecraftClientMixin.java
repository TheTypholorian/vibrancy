package net.typho.vibrancy.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.typho.vibrancy.AlphaWarningScreen;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(
            method = "createInitScreens",
            at = @At("TAIL")
    )
    private void createInitScreens(List<Function<Runnable, Screen>> list, CallbackInfo ci) {
        if (!Vibrancy.SEEN_ALPHA_TEXT) {
            list.add(AlphaWarningScreen::new);
        }
    }
}
