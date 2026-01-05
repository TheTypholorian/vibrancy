package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.typho.vibrancy.sodium.ChunkVertexEncoderVertexExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author Developers of Veil
 */
@Pseudo
@Mixin(ChunkVertexEncoder.Vertex.class)
public class ChunkVertexEncoderVertexMixin implements ChunkVertexEncoderVertexExtension {
    @Unique
    private int vibrancy$packedNormal;

    @Override
    public int vibrancy$getPackedNormal() {
        return vibrancy$packedNormal;
    }

    @Override
    public void vibrancy$setNormal(int packedNormal) {
        vibrancy$packedNormal = packedNormal;
    }
}
