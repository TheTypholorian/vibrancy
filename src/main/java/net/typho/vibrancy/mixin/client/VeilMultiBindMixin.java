package net.typho.vibrancy.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foundry.veil.api.client.render.ext.VeilMultiBind;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VeilMultiBind.class, remap = false)
public class VeilMultiBindMixin {
    @WrapOperation(
            method = "getTarget",
            at = @At(
                    value = "FIELD",
                    target = "Lorg/lwjgl/opengl/GLCapabilities;glGetTextureParameteriv:J"
            )
    )
    private static long getTarget(GLCapabilities instance, Operation<Long> original) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return 0;
        }

        return original.call(instance);
    }
}
