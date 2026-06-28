package net.typho.vibrancy.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer;
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects;
import net.typho.vibrancy.RenderRegionExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderRegion.class)
public class RenderRegionMixin implements RenderRegionExtension {
    @Unique
    private GpuBuffer vibrancy$lightBuffer;
    @Unique
    private GpuBuffer vibrancy$shadowBuffer;

    @Override
    @Nullable
    public GpuBuffer getVibrancy$lightBuffer() {
        return vibrancy$lightBuffer;
    }

    @Override
    public void setVibrancy$lightBuffer(@Nullable GpuBuffer gpuBuffer) {
        vibrancy$lightBuffer = gpuBuffer;
    }

    @Override
    @Nullable
    public GpuBuffer getVibrancy$shadowBuffer() {
        return vibrancy$shadowBuffer;
    }

    @Override
    public void setVibrancy$shadowBuffer(@Nullable GpuBuffer gpuBuffer) {
        vibrancy$shadowBuffer = gpuBuffer;
    }

    @Inject(
            method = "delete",
            at = @At("TAIL")
    )
    private void delete(CallbackInfo ci) {
        if (vibrancy$lightBuffer != null) {
            vibrancy$lightBuffer.recycle();
            vibrancy$lightBuffer = null;
        }

        if (vibrancy$shadowBuffer != null) {
            vibrancy$shadowBuffer.recycle();
            vibrancy$shadowBuffer = null;
        }
    }
}
