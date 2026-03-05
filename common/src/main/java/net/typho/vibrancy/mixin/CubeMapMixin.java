package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.CubeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(CubeMap.class)
public class CubeMapMixin {
    @ModifyConstant(
            method = "render",
            constant = @Constant(floatValue = 256)
    )
    private float renderX(float constant) {
        return constant * 4;
    }
}
