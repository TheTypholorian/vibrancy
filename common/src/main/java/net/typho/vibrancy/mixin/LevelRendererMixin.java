package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.typho.vibrancy.Vibrancy;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"
            )
    )
    private void render(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Vibrancy.INSTANCE.render();
    }

    @Inject(
            method = "prepareCullFrustum",
            at = @At("HEAD")
    )
    private void prepareCullFrustum(Vec3 cameraPosition, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Vibrancy.iProjMat = new Matrix4f(projectionMatrix).invertPerspective();
        Vibrancy.iModelMat = new Matrix4f(frustumMatrix)
                .mulLocal(new Matrix4f(Vibrancy.iProjMat).mul(RenderSystem.getProjectionMatrix()))
                .invert();
        Vibrancy.camera = cameraPosition.toVector3f();
    }

    @ModifyConstant(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            constant = @Constant(intValue = 15728880)
    )
    private static int getLightColor(int constant) {
        return LightTexture.FULL_BLOCK;
    }
}
