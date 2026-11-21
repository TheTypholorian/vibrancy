package net.typho.vibrancy.mixin.normal;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.typho.vibrancy.ext.ChunkVertexExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkVertexEncoder.Vertex.class)
public class ChunkVertexMixin implements ChunkVertexExt {
    @Unique
    private int vibrancy$normal = 0;

    @Override
    public int vibrancy$getNormal() {
        return vibrancy$normal;
    }

    @Override
    public void vibrancy$setNormal(int normal) {
        vibrancy$normal = normal;
    }

    @Inject(
            method = "copyVertexTo",
            at = @At("TAIL")
    )
    private static void copyVertexTo(
            ChunkVertexEncoder.Vertex from,
            ChunkVertexEncoder.Vertex to,
            CallbackInfo ci
    ) {
        ((ChunkVertexExt) to).vibrancy$setNormal(((ChunkVertexExt) from).vibrancy$getNormal());
    }
}
