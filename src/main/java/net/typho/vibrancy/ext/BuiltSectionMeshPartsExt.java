package net.typho.vibrancy.ext;

import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

public interface BuiltSectionMeshPartsExt {
    NativeBuffer vibrancy$getOverlayIndexBuffer();

    void vibrancy$setOverlayIndexBuffer(NativeBuffer buffer);
}
