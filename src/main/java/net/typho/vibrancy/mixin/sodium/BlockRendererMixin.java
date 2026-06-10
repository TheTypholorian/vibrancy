package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas;
import net.typho.big_shot_lib.api.math.vec.NeoVec3f;
import net.typho.vibrancy.shadows.LightFace;
import net.typho.vibrancy.shadows.PrimitiveVertex;
import net.typho.vibrancy.util.SectionMeshCache;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderer.class)
public class BlockRendererMixin {
    @Shadow
    private ChunkBuildBuffers buffers;
    @Unique
    private BlockPos vibrancy$block;
    @Unique
    private NeoAtlas vibrancy$atlas;

    @Inject(
            method = "renderModel",
            at = @At("HEAD")
    )
    private void renderModel(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        vibrancy$block = pos;
    }

    @Inject(
            method = "prepare",
            at = @At("TAIL")
    )
    private void prepare(ChunkBuildBuffers buffers, LevelSlice level, TranslucentGeometryCollector collector, CallbackInfo ci) {
        vibrancy$atlas = NeoAtlas.Companion.getBlocks();
    }

    @Unique
    private PrimitiveVertex vibrancy$convertVertex(ChunkVertexEncoder.Vertex vertex, int normal, float offX, float offY, float offZ) {
        return new PrimitiveVertex(
                vertex.x + offX,
                vertex.y + offY,
                vertex.z + offZ,
                vertex.color,
                vertex.u,
                vertex.v,
                vertex.light,
                normal
        );
    }

    @Inject(
            method = "bufferQuad",
            at = @At("TAIL")
    )
    private void bufferQuad(
            MutableQuadViewImpl quad,
            float[] brightnesses,
            Material material,
            CallbackInfo ci,
            @Local ChunkVertexEncoder.Vertex[] vertices
    ) {
        SectionMeshCache cache = ((SectionMeshCache.Holder) buffers).getVibrancy$sectionMeshCache();

        if (cache != null && vibrancy$block != null) {
            float offX = -SectionPos.sectionRelative(vibrancy$block.getX());
            float offY = -SectionPos.sectionRelative(vibrancy$block.getY());
            float offZ = -SectionPos.sectionRelative(vibrancy$block.getZ());

            cache.getOrCreate(vibrancy$block).get(material.isTranslucent()).add(
                    new LightFace(
                            vibrancy$convertVertex(vertices[0], quad.getVertexNormal(0), offX, offY, offZ),
                            vibrancy$convertVertex(vertices[1], quad.getVertexNormal(1), offX, offY, offZ),
                            vibrancy$convertVertex(vertices[2], quad.getVertexNormal(2), offX, offY, offZ),
                            vibrancy$convertVertex(vertices[3], quad.getVertexNormal(3), offX, offY, offZ),
                            vibrancy$atlas
                    )
            );
        }
    }
}
