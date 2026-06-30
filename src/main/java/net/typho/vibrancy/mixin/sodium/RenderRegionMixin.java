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
    private boolean vibrancy$initialized = false;
    @Unique
    private GpuBuffer vibrancy$lightBuffer;
    @Unique
    private GpuBuffer vibrancy$shadowBuffer;
    @Unique
    private GpuBuffer vibrancy$gridBuffer;

    @Override
    public boolean getVibrancy$initialized() {
        return vibrancy$initialized;
    }

    @Override
    public void setVibrancy$initialized(boolean b) {
        vibrancy$initialized = b;
    }

    @Override
    @Nullable
    public GpuBuffer getVibrancy$lightBuffer() {
        return vibrancy$lightBuffer;
    }

    @Override
    public void setVibrancy$lightBuffer(@Nullable GpuBuffer gpuBuffer) {
        if (vibrancy$lightBuffer != null) {
            vibrancy$lightBuffer.recycle();
        }

        vibrancy$lightBuffer = gpuBuffer;
    }

    @Override
    @Nullable
    public GpuBuffer getVibrancy$shadowBuffer() {
        return vibrancy$shadowBuffer;
    }

    @Override
    public void setVibrancy$shadowBuffer(@Nullable GpuBuffer gpuBuffer) {
        if (vibrancy$shadowBuffer != null) {
            vibrancy$shadowBuffer.recycle();
        }

        vibrancy$shadowBuffer = gpuBuffer;
    }

    @Override
    @Nullable
    public GpuBuffer getVibrancy$gridBuffer() {
        return vibrancy$gridBuffer;
    }

    @Override
    public void setVibrancy$gridBuffer(@Nullable GpuBuffer gpuBuffer) {
        if (vibrancy$gridBuffer != null) {
            vibrancy$gridBuffer.recycle();
        }

        vibrancy$gridBuffer = gpuBuffer;
    }

    @Inject(
            method = "delete",
            at = @At("TAIL")
    )
    private void delete(CallbackInfo ci) {
        vibrancy$clear();
        vibrancy$initialized = false;
    }
}
