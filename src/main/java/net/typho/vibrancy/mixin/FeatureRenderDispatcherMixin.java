package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.typho.vibrancy.entity.VibrancyEntityShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherMixin {
    @Shadow
    @Final
    private FeatureRendererMap featureRenderers;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBuffers;stagedVertexBuffer()Lnet/minecraft/client/renderer/StagedVertexBuffer;"
            )
    )
    private StagedVertexBuffer init(RenderBuffers instance, Operation<StagedVertexBuffer> original) {
        if ((Object) this instanceof VibrancyEntityShadowFeatureRenderer.Dispatcher) {
            return new VibrancyEntityShadowFeatureRenderer.VertexBuffer(() -> "Vibrancy Entity Shadow Collection", 786432);
        } else {
            return original.call(instance);
        }
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void init(
            RenderBuffers renderBuffers,
            ModelManager modelManager,
            AtlasManager atlasManager,
            Font font,
            GameRenderState gameRenderState,
            CallbackInfo ci
    ) {
        featureRenderers.put(VibrancyEntityShadowFeatureRenderer.TYPE, new VibrancyEntityShadowFeatureRenderer());
    }
}
