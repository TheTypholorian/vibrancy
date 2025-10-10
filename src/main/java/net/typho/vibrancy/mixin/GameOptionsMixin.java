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
        visitor.accept("vibrancy/dynamic_lightmap", Vibrancy.DYNAMIC_LIGHTMAP);
        visitor.accept("vibrancy/transparency_test", Vibrancy.TRANSPARENCY_TEST);
        //visitor.accept("vibrancy/better_sky", Vibrancy.BETTER_SKY);
        visitor.accept("vibrancy/better_fog", Vibrancy.BETTER_FOG);
        visitor.accept("vibrancy/elytra_trails", Vibrancy.ELYTRA_TRAILS);
        visitor.accept("vibrancy/raytrace_distance", Vibrancy.RAYTRACE_DISTANCE);
        visitor.accept("vibrancy/light_cull_distance", Vibrancy.LIGHT_CULL_DISTANCE);
        visitor.accept("vibrancy/max_raytraced_lights", Vibrancy.MAX_RAYTRACED_LIGHTS);
        visitor.accept("vibrancy/max_shadow_distance", Vibrancy.MAX_SHADOW_DISTANCE);
        visitor.accept("vibrancy/max_light_radius", Vibrancy.MAX_LIGHT_RADIUS);
        visitor.accept("vibrancy/block_light_multiplier", Vibrancy.BLOCK_LIGHT_MULTIPLIER);
    }

    @Inject(
            method = "accept",
            at = @At("TAIL")
    )
    private void accept(GameOptions.Visitor visitor, CallbackInfo ci) {
        Vibrancy.SEEN_ALPHA_TEXT = visitor.visitBoolean("vibrancy/seen_alpha_text", Vibrancy.SEEN_ALPHA_TEXT);
    }
}
