package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlDebug;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

import static org.lwjgl.opengl.GL43.GL_DEBUG_SOURCE_SHADER_COMPILER;
import static org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_ERROR;

@Mixin(GlDebug.class)
public class GlDebugMixin {
    @WrapOperation(
            method = "printDebugLog",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V",
                    remap = false
            )
    )
    private static void printDebugLog(
            Logger instance,
            String s,
            Object o,
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int source,
            @Local(argsOnly = true, ordinal = 1) int type
    ) {
        if (type == GL_DEBUG_TYPE_ERROR && source != GL_DEBUG_SOURCE_SHADER_COMPILER) {
            IllegalStateException error = new IllegalStateException(o.toString());
            error.setStackTrace(Arrays.copyOfRange(error.getStackTrace(), 4, error.getStackTrace().length));
            throw error;
        } else {
            original.call(instance, s, o);
        }
    }
}