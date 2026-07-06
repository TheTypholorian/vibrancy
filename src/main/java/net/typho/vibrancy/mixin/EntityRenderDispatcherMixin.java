package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.typho.big_shot_lib.api.util.platform.PlatformUtil;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.VibrancyConfig;
import net.typho.vibrancy.entity.OrderedSubmitNodeCollectorExtension;
import net.typho.vibrancy.entity.VibrancyEntityShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
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
                ext.getVibrancy$entityShadowSubmit().submit = new VibrancyEntityShadowFeatureRenderer.Submit();

                return;
            }

            if (submitNodeCollector instanceof SubmitNodeStorage storage) {
                BlockPos pos = BlockPos.containing(renderState.x, renderState.y, renderState.z);
                PoseStack.Pose pose = poseStack.last().copy();
                pose.translate((float) camera.pos.x, (float) camera.pos.y, (float) camera.pos.z);
                storage.order(0).shadows.submit(new VibrancyEntityShadowFeatureRenderer.Submit(
                        renderer,
                        renderState,
                        pose,
                        BlockBox.of(pos.minus(1, 3, 1), pos.plus(1))
                ));
                return true;
            } else if (PlatformUtil.INSTANCE.isDevEnv()) {
                Vibrancy.LOGGER.warn("{} is not a SubmitNodeStorage, cannot submit vibrancy entity shadow", submitNodeCollector);
            }
        }

        original.call(renderState, camera, x, y, z, poseStack, submitNodeCollector);
    }
}
