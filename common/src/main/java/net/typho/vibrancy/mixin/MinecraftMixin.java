package net.typho.vibrancy.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @ModifyVariable(
            method = "grabPanoramixScreenshot",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private int panoramixWidth(int width) {
        return width * 4;
    }

    @ModifyVariable(
            method = "grabPanoramixScreenshot",
            at = @At("HEAD"),
            ordinal = 1,
            argsOnly = true
    )
    private int panoramixHeight(int height) {
        return height * 4;
    }
}
