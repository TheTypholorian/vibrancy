package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.typho.vibrancy.VibrancyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >=1.21.9 {
/*import java.util.List;
*///? }

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    //? if <1.21.9 {
    @SuppressWarnings("unchecked")
    @WrapOperation(
            //? if <1.21.5 {
            method = "render",
            //? } else {
            /*method = "render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            *///? }
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
            )
    )
    private <T> T render(OptionInstance<T> instance, Operation<T> original) {
        return VibrancyConfig.entityShadowsEnabled ? (T) (Object) false : original.call(instance);
    }
    //? } else {
    /*@WrapOperation(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;isEmpty()Z"
            )
    )
    private <T> boolean submit(List<T> instance, Operation<Boolean> original) {
        return VibrancyConfig.entityShadowsEnabled || original.call(instance);
    }
    *///? }
}
