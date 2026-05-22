package net.typho.vibrancy.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas;
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlasSprite;
import net.typho.big_shot_lib.api.client.rendering.util.quad.BasicBakedQuad;
import net.typho.big_shot_lib.api.math.NeoDirection;
import net.typho.big_shot_lib.api.math.NeoDirectionKt;
import net.typho.big_shot_lib.api.math.vec.NeoVec3i;
import net.typho.big_shot_lib.api.util.WrapperUtil;
import net.typho.vibrancy.shadows.LightFace;
import net.typho.vibrancy.util.SectionMeshCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(BlockRenderer.class)
public class BlockRendererMixin {
    @Shadow
    private ChunkBuildBuffers buffers;
    @Unique
    private BlockPos vibrancy$block;

    @Inject(
            method = "renderModel",
            at = @At("HEAD")
    )
    private void renderModel(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        vibrancy$block = pos.immutable();
    }

    @Inject(
            method = "bufferQuad",
            at = @At("TAIL")
    )
    private void bufferQuad(
            MutableQuadViewImpl quad,
            float[] brightnesses,
            Material material,
            CallbackInfo ci
    ) {
        SectionMeshCache cache = ((SectionMeshCache.Holder) buffers).getVibrancy$sectionMeshCache();

        if (cache != null && vibrancy$block != null) {
            var block = cache.get(vibrancy$block);
            block.computeIfAbsent(material.isTranslucent(), translucent -> new ArrayList<>()).add(
                    new LightFace(
                            vibrancy$block,
                            WrapperUtil.Companion.getINSTANCE().wrap(quad.toBakedQuad(null)),
                            NeoAtlas.Companion.getBlocks()
                    )
            );
        }
    }
}
