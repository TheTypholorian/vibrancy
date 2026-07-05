package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureRenderDispatcher.class)
public interface FeatureRenderDispatcherAccessor {
    @Accessor("stagedVertexBuffer")
    @NotNull StagedVertexBuffer vibrancy$getStagedVertexBuffer();

    @Accessor("featureRenderers")
    @NotNull FeatureRendererMap vibrancy$getFeatureRenderers();
}
