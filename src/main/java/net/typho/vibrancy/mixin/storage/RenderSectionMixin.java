package net.typho.vibrancy.mixin.storage;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.core.SectionPos;
import net.typho.vibrancy.ext.RenderRegionExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSection.class)
public class RenderSectionMixin {
    @Shadow
    @Final
    private RenderRegion region;
    @Shadow
    @Final
    private int chunkX;
    @Shadow
    @Final
    private int chunkY;
    @Shadow
    @Final
    private int chunkZ;

    @Inject(
            method = "setPendingUpdate",
            at = @At("TAIL")
    )
    private void setPendingUpdate(int type, long now, CallbackInfo ci) {
        ((RenderRegionExt) region).vibrancy$getBlockLightStorage().onSectionChanged(SectionPos.of(chunkX, chunkY, chunkZ));
    }
}
