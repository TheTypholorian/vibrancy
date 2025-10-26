package net.typho.vibrancy.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VideoSettingsScreen.class)
public class VideoSettingsScreenMixin {
    @Inject(
            method = "options",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void getOptions(Options options, CallbackInfoReturnable<OptionInstance<?>[]> cir) {
        OptionInstance<?>[] original = cir.getReturnValue();
        OptionInstance<?>[] arr = new OptionInstance[original.length + 9];
        System.arraycopy(original, 0, arr, 0, original.length);
        //arr[original.length - 8] = Vibrancy.BETTER_SKY;
        //arr[original.length - 9] = Vibrancy.DYNAMIC_LIGHTMAP;
        arr[original.length - 8] = Vibrancy.BETTER_FOG;
        arr[original.length - 7] = Vibrancy.ELYTRA_TRAILS;
        arr[original.length - 6] = Vibrancy.TRANSPARENCY_TEST;
        arr[original.length - 5] = Vibrancy.RAYTRACE_DISTANCE;
        arr[original.length - 4] = Vibrancy.LIGHT_CULL_DISTANCE;
        arr[original.length - 3] = Vibrancy.MAX_RAYTRACED_LIGHTS;
        arr[original.length - 2] = Vibrancy.MAX_SHADOW_DISTANCE;
        arr[original.length - 1] = Vibrancy.MAX_LIGHT_RADIUS;
        arr[original.length] = Vibrancy.BLOCK_LIGHT_MULTIPLIER;
        cir.setReturnValue(arr);
    }
}
