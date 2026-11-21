package net.typho.vibrancy.mixin.normal;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.BlockPos;
import net.typho.vibrancy.ext.ChunkVertexExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefaultFluidRenderer.class)
public class DefaultFluidRendererMixin {
    @Inject(
            method = "writeQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;getTexU(I)F"
            )
    )
    private void bufferQuad(
            ChunkModelBuilder builder,
            TranslucentGeometryCollector collector,
            Material material,
            BlockPos offset,
            ModelQuadView quad,
            ModelQuadFacing facing,
            boolean flip,
            CallbackInfo ci,
            @Local(name = "i") int i,
            @Local(name = "out") ChunkVertexEncoder.Vertex out
    ) {
        ((ChunkVertexExt) out).vibrancy$setNormal(quad.getVertexNormal(i));
    }
}
