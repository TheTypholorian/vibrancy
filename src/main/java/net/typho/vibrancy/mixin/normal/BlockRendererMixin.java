package net.typho.vibrancy.mixin.normal;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.typho.vibrancy.ext.ChunkVertexExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderer.class)
public class BlockRendererMixin {
    @Inject(
            method = "bufferQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/api/util/ColorARGB;toABGR(I)I"
            )
    )
    private void bufferQuad(
            MutableQuadViewImpl quad,
            float[] brightnesses,
            Material material,
            CallbackInfo ci,
            @Local(name = "srcIndex") int srcIndex,
            @Local(name = "out") ChunkVertexEncoder.Vertex out
    ) {
        ((ChunkVertexExt) out).vibrancy$setNormal(quad.getVertexNormal(srcIndex));
    }
}
