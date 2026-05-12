package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(NativeImage.class)
public interface NativeImageAccessor {
    @Accessor("pixels")
    long big_shot_lib$getPixels();
}
