package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.BlockPos;
import net.typho.vibrancy.util.SectionMeshCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefaultFluidRenderer.class)
public class DefaultFluidRendererMixin {
    @Inject(
            method = "writeQuad",
            at = @At("TAIL")
    )
    private void writeQuad(
            ChunkModelBuilder builder,
            TranslucentGeometryCollector collector,
            Material material,
            BlockPos offset,
            ModelQuadView quad,
            ModelQuadFacing facing,
            boolean flip,
            CallbackInfo ci,
            @Local ChunkVertexEncoder.Vertex[] vertices
    ) {
        var consumer = ((SectionMeshCache.ConsumerExtension) builder).getVibrancy$sectionMeshConsumer();

        if (consumer != null) {
            int index = 0;

            for (ChunkVertexEncoder.Vertex vertex : vertices) {
                consumer.addVertex(vertex.x, vertex.y, vertex.z)
                        .setColor(vertex.color)
                        .setUv(vertex.u, vertex.v)
                        .setLight(vertex.light)
                        .setNormal(quad.getVertexNormal(index++));
            }
            consumer.flush();
        }
    }
}
