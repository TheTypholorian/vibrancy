package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.typho.vibrancy.VibrancyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @SuppressWarnings("UNCHECKED_CAST")
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
            )
    )
    private <T> T render(OptionInstance<T> instance, Operation<T> original) {
        return VibrancyConfig.entityShadowsEnabled ? (T) (Object) false : original.call(instance);
    }
}
