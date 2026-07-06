package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.typho.vibrancy.entity.OrderedSubmitNodeCollectorExtension;
import net.typho.vibrancy.entity.VibrancyEntityShadowFeatureRenderer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageMixin implements OrderedSubmitNodeCollectorExtension {
    @Shadow
    @Final
    private Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder;
    @Unique
    private VibrancyEntityShadowFeatureRenderer.SubmitRef vibrancy$entityShadowSubmit = new VibrancyEntityShadowFeatureRenderer.SubmitRef();

    @Shadow
    public abstract SubmitNodeCollection order(int order);

    @Override
    public @NotNull VibrancyEntityShadowFeatureRenderer.SubmitRef getVibrancy$entityShadowSubmit() {
        return vibrancy$entityShadowSubmit;
    }

    @Override
    public void setVibrancy$entityShadowSubmit(@NotNull VibrancyEntityShadowFeatureRenderer.SubmitRef submitRef) {
        vibrancy$entityShadowSubmit = submitRef;

        for (SubmitNodeCollection collection : submitsPerOrder.values()) {
            ((OrderedSubmitNodeCollectorExtension) collection).setVibrancy$entityShadowSubmit(submitRef);
        }
    }

    @Override
    public void vibrancy$submitEntityShadow() {
        if (vibrancy$entityShadowSubmit.submit != null) {
            order(0).shadows.submit(vibrancy$entityShadowSubmit.submit);
            vibrancy$entityShadowSubmit.submit = null;
        }
    }

    @ModifyReturnValue(
            method = "order(I)Lnet/minecraft/client/renderer/SubmitNodeCollection;",
            at = @At("RETURN")
    )
    private SubmitNodeCollection order(SubmitNodeCollection original) {
        ((OrderedSubmitNodeCollectorExtension) original).setVibrancy$entityShadowSubmit(vibrancy$entityShadowSubmit);
        return original;
    }
}
