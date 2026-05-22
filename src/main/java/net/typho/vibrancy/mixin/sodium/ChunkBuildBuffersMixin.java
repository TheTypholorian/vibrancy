package net.typho.vibrancy.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.typho.vibrancy.util.SectionMeshCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkBuildBuffers.class)
public class ChunkBuildBuffersMixin implements SectionMeshCache.Holder {
    @Unique
    private SectionMeshCache vibrancy$sectionMeshCache;

    @Override
    public @Nullable SectionMeshCache getVibrancy$sectionMeshCache() {
        return vibrancy$sectionMeshCache;
    }

    @Override
    public void setVibrancy$sectionMeshCache(@Nullable SectionMeshCache sectionMeshCache) {
        vibrancy$sectionMeshCache = sectionMeshCache;
    }
}
