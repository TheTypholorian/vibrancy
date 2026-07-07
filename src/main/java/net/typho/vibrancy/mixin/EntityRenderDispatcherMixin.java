package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.typho.vibrancy.VibrancyConfig;
import net.typho.vibrancy.entity.OrderedSubmitNodeCollectorExtension;
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
    private boolean submit(List<EntityRenderState.ShadowPiece> instance, Operation<Boolean> original) {
        return original.call(instance) || VibrancyConfig.entityShadowsEnabled;
    }

    @WrapMethod(
            method = "submit"
    )
    private void submit(
            EntityRenderState renderState,
            CameraRenderState camera,
            double x,
            double y,
            double z,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            Operation<Void> original
    ) {
        if (VibrancyConfig.entityShadowsEnabled) {
            var ext = OrderedSubmitNodeCollectorExtension.get(submitNodeCollector);

            if (ext != null) {
                double radius = (renderState.boundingBoxWidth + 1) / 2;
                ext.getVibrancy$entityShadowSubmit().submit = new VibrancyEntityShadowFeatureRenderer.Submit(new BlockBox(
                        BlockPos.containing(renderState.x - radius, renderState.y - 4, renderState.z - radius),
                        BlockPos.containing(renderState.x + radius, renderState.y + renderState.boundingBoxHeight + 1, renderState.z + radius)
                ));
                original.call(renderState, camera, x, y, z, poseStack, submitNodeCollector);
                ext.vibrancy$submitEntityShadow();

                return;
            }
        }

        original.call(renderState, camera, x, y, z, poseStack, submitNodeCollector);
    }
}
