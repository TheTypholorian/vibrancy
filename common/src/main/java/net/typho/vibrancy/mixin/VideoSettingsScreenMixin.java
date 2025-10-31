package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VideoSettingsScreen.class)
public class VideoSettingsScreenMixin {
    @ModifyReturnValue(
            method = "options",
            at = @At("RETURN")
    )
    private static OptionInstance<?>[] getOptions(OptionInstance<?>[] original) {
        OptionInstance<?>[] arr = new OptionInstance[original.length + 9];
        System.arraycopy(original, 0, arr, 0, original.length);
        //arr[original.length - 8] = Vibrancy.BETTER_SKY;
        //arr[original.length - 9] = Vibrancy.DYNAMIC_LIGHTMAP;
        arr[original.length - 8] = Vibrancy.BETTER_FOG;
        arr[original.length - 7] = Vibrancy.ELYTRA_TRAILS;
        arr[original.length - 6] = Vibrancy.TRANSPARENCY_TEST;
        arr[original.length - 5] = Vibrancy.SKY_SHADOW_DISTANCE;
        arr[original.length - 4] = Vibrancy.RAYTRACE_DISTANCE;
        arr[original.length - 3] = Vibrancy.LIGHT_CULL_DISTANCE;
        arr[original.length - 2] = Vibrancy.MAX_RAYTRACED_LIGHTS;
        arr[original.length - 1] = Vibrancy.MAX_SHADOW_DISTANCE;
        arr[original.length] = Vibrancy.MAX_LIGHT_RADIUS;
        return arr;
    }
}
