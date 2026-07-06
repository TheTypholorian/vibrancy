package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.typho.vibrancy.entity.OrderedSubmitNodeCollectorExtension;
import net.typho.vibrancy.entity.VibrancyEntityShadowFeatureRenderer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleFeatureRenderPhase.class)
public class SimpleFeatureRenderPhaseMixin implements OrderedSubmitNodeCollectorExtension {
    @Unique
    private VibrancyEntityShadowFeatureRenderer.SubmitRef vibrancy$entityShadowSubmit = new VibrancyEntityShadowFeatureRenderer.SubmitRef();

    @Override
    public @NotNull VibrancyEntityShadowFeatureRenderer.SubmitRef getVibrancy$entityShadowSubmit() {
        return vibrancy$entityShadowSubmit;
    }

    @Override
    public void setVibrancy$entityShadowSubmit(@NotNull VibrancyEntityShadowFeatureRenderer.SubmitRef submitRef) {
        vibrancy$entityShadowSubmit = submitRef;
    }

    @Override
    public void vibrancy$submitEntityShadow() {
        throw new UnsupportedOperationException("Cannot call vibrancy$submitEntityShadow on a FeatureRenderPhase, must call it on an OrderedSubmitNodeCollector");
    }

    @Inject(
            method = "submit",
            at = @At("TAIL")
    )
    private void submit(SubmitNode submit, CallbackInfo ci) {
        if (vibrancy$entityShadowSubmit.submit != null) {
            vibrancy$entityShadowSubmit.submit.order(0).solid.submit(submit);
        }
    }
}
