package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.typho.vibrancy.entity.OrderedSubmitNodeCollectorExtension;
import net.typho.vibrancy.entity.VibrancyEntityShadowFeatureRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectionMixin implements OrderedSubmitNodeCollectorExtension {
    @Shadow
    @Final
    public SimpleFeatureRenderPhase shadows;

    @Shadow
    public abstract List<FeatureRenderPhase<?>> allPhases();

    @Unique
    private VibrancyEntityShadowFeatureRenderer.SubmitRef vibrancy$entityShadowSubmit = new VibrancyEntityShadowFeatureRenderer.SubmitRef();

    @Override
    public @NotNull VibrancyEntityShadowFeatureRenderer.SubmitRef getVibrancy$entityShadowSubmit() {
        return vibrancy$entityShadowSubmit;
    }

    @Override
    public void setVibrancy$entityShadowSubmit(@NotNull VibrancyEntityShadowFeatureRenderer.SubmitRef submitRef) {
        vibrancy$entityShadowSubmit = submitRef;

        for (FeatureRenderPhase<?> phase : allPhases()) {
            var ext = OrderedSubmitNodeCollectorExtension.get(phase);

            if (ext != null) {
                ext.setVibrancy$entityShadowSubmit(submitRef);
            }
        }
    }

    @Override
    public void vibrancy$submitEntityShadow() {
        if (vibrancy$entityShadowSubmit.submit != null) {
            var submit = vibrancy$entityShadowSubmit.submit;
            vibrancy$entityShadowSubmit.submit = null;
            shadows.submit(submit);
        }
    }
}
