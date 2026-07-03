package net.typho.vibrancy.mixin.sodium.normal;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.typho.vibrancy.sodium.SodiumChunkVertexExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkVertexEncoder.Vertex.class)
public class ChunkVertexEncoderVertexMixin implements SodiumChunkVertexExtension {
    @Unique
    private int vibrancy$normal;

    @Override
    public int getVibrancy$normal() {
        return vibrancy$normal;
    }

    @Override
    public void setVibrancy$normal(int i) {
        vibrancy$normal = i;
    }

    @Inject(
            method = "copyVertexTo",
            at = @At("TAIL")
    )
    private static void copyVertexTo(ChunkVertexEncoder.Vertex from, ChunkVertexEncoder.Vertex _to, CallbackInfo ci) {
        ((SodiumChunkVertexExtension) _to).setVibrancy$normal(((SodiumChunkVertexExtension) from).getVibrancy$normal());
    }
}
