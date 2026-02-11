package net.typho.vibrancy.mixin;

import net.typho.big_shot_lib.api.shaders.variables.ShaderPrimitiveType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ShaderPrimitiveType.class, remap = false)
public class ShaderPrimitiveTypeMixin {
    @ModifyConstant(
            method = "matches",
            constant = @Constant(intValue = 2)
    )
    private int matches(int constant) {
        return 1;
    }
}
