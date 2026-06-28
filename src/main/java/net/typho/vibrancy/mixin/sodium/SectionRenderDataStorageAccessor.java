package net.typho.vibrancy.mixin.sodium;

import net.caffeinemc.mods.sodium.client.gpu.arena.GlBufferSegment;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionRenderDataStorage.class)
public interface SectionRenderDataStorageAccessor {
    @Accessor("vertexAllocations")
    @Nullable GlBufferSegment[] vibrancy$getVertexAllocations();
}
