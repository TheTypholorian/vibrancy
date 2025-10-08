package net.typho.vibrancy.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.typho.vibrancy.RaytracedPointEntityLight;
import net.typho.vibrancy.Vibrancy;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureRendererMixin<T extends LivingEntity> {
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD")
    )
    private void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T entity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        RaytracedPointEntityLight eLight = Vibrancy.ENTITY_LIGHTS.get(entity);

        if (eLight != null) {
            eLight.render = false;
        }
    }

    @Inject(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RaytracedPointEntityLight eLight = Vibrancy.ENTITY_LIGHTS.get(entity);

        if (eLight != null && eLight.init(stack)) {
            matrices.push();

            MinecraftClient.getInstance().getItemRenderer().getModel(stack, entity.getWorld(), entity, 0).getTransformation().getTransformation(transformationMode).apply(arm == Arm.LEFT, matrices);
            matrices.translate(-0.5f, -0.5f, -0.5f);

            float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
            Vec3d pos = entity.getCameraPosVec(tickDelta);

            Vector3f vec = matrices.peek().getPositionMatrix().transformPosition(0.5f, 0.5f, 0.5f, new Vector3f());
            eLight.setPosition(vec.x + pos.x, vec.y + pos.y, vec.z + pos.z);
            matrices.pop();
        }
    }
}
