package net.typho.vibrancy.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.hud.DebugHud;
import net.typho.vibrancy.client.DynamicLightInfo;
import net.typho.vibrancy.client.VibrancyClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = DebugHud.class, priority = 1100)
public class DebugHudMixin {
    @ModifyReturnValue(
            method = "getLeftText",
            at = @At("RETURN")
    )
    private List<String> getLeftText(List<String> original) {
        original.add("");
        original.add(VibrancyClient.DYNAMIC_LIGHT_INFOS.size() + " dynamic lights");

        for (DynamicLightInfo info : VibrancyClient.DYNAMIC_LIGHT_INFOS) {
            original.add("  " + info.toString());
        }

        return original;
    }
}
