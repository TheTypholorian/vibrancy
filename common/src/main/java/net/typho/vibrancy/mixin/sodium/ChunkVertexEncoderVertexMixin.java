package net.typho.vibrancy.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.typho.vibrancy.sodium.VertexWithNormal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ChunkVertexEncoder.Vertex.class, remap = false)
public class ChunkVertexEncoderVertexMixin implements VertexWithNormal {
    @Unique
    private int vibrancy$normal;

    @Override
    public int vibrancy$getNormal() {
        return vibrancy$normal;
    }

    @Override
    public void vibrancy$setNormal(int n) {
        vibrancy$normal = n;
    }
}
