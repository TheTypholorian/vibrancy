package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.typho.vibrancy.VibrancyRenderPasses;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefaultTerrainRenderPasses.class)
public class DefaultTerrainRenderPassesMixin {
    @Shadow
    @Final
    @Mutable
    public static TerrainRenderPass[] ALL;

    @Shadow
    @Final
    public static TerrainRenderPass SOLID;

    @Shadow
    @Final
    public static TerrainRenderPass CUTOUT;

    @Shadow
    @Final
    public static TerrainRenderPass TRANSLUCENT;

    @Inject(
            method = "<clinit>",
            at = @At("TAIL")
    )
    private static void clinit(CallbackInfo ci) {
        TerrainRenderPass[] array = new TerrainRenderPass[ALL.length + VibrancyRenderPasses.ALL.length];
        System.arraycopy(ALL, 0, array, 0, ALL.length);
        System.arraycopy(VibrancyRenderPasses.ALL, 0, array, ALL.length, VibrancyRenderPasses.ALL.length);
        ALL = array;
    }

    @Overwrite
    public static int getPassIndex(TerrainRenderPass pass) {
        if (pass == SOLID) {
            return 0;
        } else if (pass == VibrancyRenderPasses.SOLID) {
            return 1;
        } else if (pass == CUTOUT) {
            return 2;
        } else if (pass == VibrancyRenderPasses.CUTOUT) {
            return 3;
        } else if (pass == TRANSLUCENT) {
            return 4;
        } else if (pass == VibrancyRenderPasses.TRANSLUCENT) {
            return 5;
        } else {
            throw new IllegalArgumentException("Unknown terrain render pass");
        }
    }
}
