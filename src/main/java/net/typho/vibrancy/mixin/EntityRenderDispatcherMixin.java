package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.typho.big_shot_lib.api.util.platform.PlatformUtil;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.VibrancyConfig;
import net.typho.vibrancy.entity.VibrancyEntityShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @WrapOperation(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;isEmpty()Z"
            )
    )
    private <S extends EntityRenderState> boolean submit(
            List<EntityRenderState.ShadowPiece> list,
            Operation<Boolean> original,
            @Local(argsOnly = true) S renderState,
            @Local(argsOnly = true) PoseStack poseStack,
            @Local(argsOnly = true) SubmitNodeCollector submitNodeCollector,
            @Local EntityRenderer<?, ? super S> renderer
    ) {
        if (VibrancyConfig.entityShadowsEnabled) {
            if (submitNodeCollector instanceof SubmitNodeStorage storage) {
                BlockPos pos = BlockPos.containing(renderState.x, renderState.y, renderState.z);
                storage.order(0).shadows.submit(new VibrancyEntityShadowFeatureRenderer.Submit<S>(
                        renderer,
                        renderState,
                        poseStack.last().copy(),
                        BlockBox.of(pos.minus(3), pos.plus(3))
                ));
                return true;
            } else if (PlatformUtil.INSTANCE.isDevEnv()) {
                Vibrancy.LOGGER.warn("{} is not a SubmitNodeStorage, cannot submit vibrancy entity shadow", submitNodeCollector);
            }
        }

        return original.call(list);
    }
}
