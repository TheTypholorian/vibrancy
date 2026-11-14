package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlTessellation;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.typho.vibrancy.ext.DeviceResourcesExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderRegion.DeviceResources.class)
public class DeviceResourcesMixin implements DeviceResourcesExt {
    @Unique
    private GlTessellation vibrancy$overlayTessellation;

    @Override
    public GlTessellation vibrancy$getOverlayTessellation() {
        return vibrancy$overlayTessellation;
    }

    @Override
    public void vibrancy$updateOverlayTessellation(CommandList commandList, GlTessellation tessellation) {
        if (vibrancy$overlayTessellation != null) {
            vibrancy$overlayTessellation.delete(commandList);
        }

        vibrancy$overlayTessellation = tessellation;
    }

    @Override
    public void vibrancy$deleteOverlayTessellation(CommandList commandList) {
        if (vibrancy$overlayTessellation != null) {
            vibrancy$overlayTessellation.delete(commandList);
            vibrancy$overlayTessellation = null;
        }
    }

    @Inject(
            method = "delete",
            at = @At("HEAD")
    )
    private void delete(CommandList commandList, CallbackInfo ci) {
        vibrancy$deleteOverlayTessellation(commandList);
    }
}
