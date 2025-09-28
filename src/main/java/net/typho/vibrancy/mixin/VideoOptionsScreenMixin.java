package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VideoOptionsScreen.class)
public class VideoOptionsScreenMixin {
    @ModifyReturnValue(
            method = "getOptions",
            at = @At("RETURN")
    )
    private static SimpleOption<?>[] getOptions(SimpleOption<?>[] original) {
        SimpleOption<?>[] arr = new SimpleOption[original.length + 5];
        System.arraycopy(original, 0, arr, 0, original.length);
        arr[original.length - 4] = Vibrancy.DYNAMIC_LIGHTMAP;
        arr[original.length - 3] = Vibrancy.RAYTRACE_DISTANCE;
        arr[original.length - 2] = Vibrancy.LIGHT_CULL_DISTANCE;
        arr[original.length - 1] = Vibrancy.MAX_RAYTRACED_LIGHTS;
        arr[original.length] = Vibrancy.MAX_SHADOW_DISTANCE;
        return arr;
    }
}
