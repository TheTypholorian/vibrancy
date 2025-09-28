package net.typho.vibrancy.mixin;

import net.minecraft.client.option.GameOptions;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Inject(
            method = "acceptProfiledOptions",
            at = @At("TAIL")
    )
    private void acceptProfiledOptions(GameOptions.OptionVisitor visitor, CallbackInfo ci) {
        visitor.accept("vibrancy/raytrace_distance", Vibrancy.RAYTRACE_DISTANCE);
        visitor.accept("vibrancy/light_cull_distance", Vibrancy.LIGHT_CULL_DISTANCE);
        visitor.accept("vibrancy/max_raytraced_lights", Vibrancy.MAX_RAYTRACED_LIGHTS);
        visitor.accept("vibrancy/max_shadow_distance", Vibrancy.MAX_SHADOW_DISTANCE);
    }

    @Inject(
            method = "accept",
            at = @At("TAIL")
    )
    private void accept(GameOptions.Visitor visitor, CallbackInfo ci) {
        Vibrancy.SEEN_ALPHA_TEXT = visitor.visitBoolean("vibrancy/seen_alpha_text", Vibrancy.SEEN_ALPHA_TEXT);
    }
}
