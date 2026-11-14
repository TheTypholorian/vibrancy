package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.typho.vibrancy.ext.BuiltSectionMeshPartsExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BuiltSectionMeshParts.class)
public class BuiltSectionMeshPartsMixin implements BuiltSectionMeshPartsExt {
    @Unique
    private NativeBuffer vibrancy$overlayIndexBuffer;

    @Override
    public NativeBuffer vibrancy$getOverlayIndexBuffer() {
        return vibrancy$overlayIndexBuffer;
    }

    @Override
    public void vibrancy$setOverlayIndexBuffer(NativeBuffer buffer) {
        vibrancy$overlayIndexBuffer = buffer;
    }
}
