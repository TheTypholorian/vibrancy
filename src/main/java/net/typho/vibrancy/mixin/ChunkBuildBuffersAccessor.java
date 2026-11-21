package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkBuildBuffers.class)
public interface ChunkBuildBuffersAccessor {
    @Accessor("vertexType")
    ChunkVertexType vibrancy$vertexType();
}
