package net.typho.vibrancy.ext;

import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlTessellation;

public interface DeviceResourcesExt {
    GlTessellation vibrancy$getOverlayTessellation();

    void vibrancy$updateOverlayTessellation(CommandList commandList, GlTessellation tessellation);

    void vibrancy$deleteOverlayTessellation(CommandList commandList);
}
