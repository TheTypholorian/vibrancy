package net.typho.vibrancy.mixin;

import net.minecraft.client.Options;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class OptionsMixin {
    @Inject(
            method = "processOptions",
            at = @At("TAIL")
    )
    private void accept(Options.FieldAccess access, CallbackInfo ci) {
        //access.accept("vibrancy/dynamic_lightmap", Vibrancy.DYNAMIC_LIGHTMAP);
        access.process("vibrancy/transparency_test", Vibrancy.TRANSPARENCY_TEST);
        //access.accept("vibrancy/better_sky", Vibrancy.BETTER_SKY);
        access.process("vibrancy/better_fog", Vibrancy.BETTER_FOG);
        access.process("vibrancy/elytra_trails", Vibrancy.ELYTRA_TRAILS);
        access.process("vibrancy/raytrace_distance", Vibrancy.RAYTRACE_DISTANCE);
        access.process("vibrancy/light_cull_distance", Vibrancy.LIGHT_CULL_DISTANCE);
        access.process("vibrancy/max_raytraced_lights", Vibrancy.MAX_RAYTRACED_LIGHTS);
        access.process("vibrancy/max_shadow_distance", Vibrancy.MAX_SHADOW_DISTANCE);
        access.process("vibrancy/max_light_radius", Vibrancy.MAX_LIGHT_RADIUS);
        access.process("vibrancy/block_light_multiplier", Vibrancy.BLOCK_LIGHT_MULTIPLIER);
    }
}
