package net.typho.vibrancy.mixin.fabric.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.typho.vibrancy.sodium.VertexWithNormal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public class BlockRendererMixin {
    @Inject(
            method = "bufferQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;lightmap(I)I"
            )
    )
    private void bufferQuad(
            MutableQuadViewImpl quad,
            float[] brightnesses,
            Material material,
            CallbackInfo ci,
            @Local ChunkVertexEncoder.Vertex vertex
    ) {
        ((VertexWithNormal) vertex).vibrancy$setNormal(quad.packedFaceNormal());
    }
}
