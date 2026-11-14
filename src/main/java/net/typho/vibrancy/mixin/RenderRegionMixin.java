package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.typho.vibrancy.ext.RenderRegionExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RenderRegion.class)
public class RenderRegionMixin implements RenderRegionExt {
    @Unique
    private final Map<TerrainRenderPass, SectionRenderDataStorage> vibrancy$sectionOverlayRenderData = new Reference2ReferenceOpenHashMap<>();
    @Unique
    private final Map<TerrainRenderPass, MultiDrawBatch> vibrancy$cachedOverlayBatches = new Reference2ReferenceOpenHashMap<>();

    @Override
    public SectionRenderDataStorage vibrancy$getOverlayStorage(TerrainRenderPass pass) {
        return vibrancy$sectionOverlayRenderData.get(pass);
    }

    @Override
    public SectionRenderDataStorage vibrancy$getOrCreateOverlayStorage(TerrainRenderPass pass) {
        var storage = vibrancy$sectionOverlayRenderData.get(pass);

        if (storage == null) {
            storage = new SectionRenderDataStorage(true);
            vibrancy$sectionOverlayRenderData.put(pass, storage);
        }

        return storage;
    }

    @Override
    public MultiDrawBatch vibrancy$getCachedOverlayBatch(TerrainRenderPass pass) {
        MultiDrawBatch batch = vibrancy$cachedOverlayBatches.get(pass);

        if (batch != null) {
            return batch;
        }

        batch = new MultiDrawBatch((ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE) + 1);
        vibrancy$cachedOverlayBatches.put(pass, batch);
        return batch;
    }

    @Inject(
            method = "delete",
            at = @At("HEAD")
    )
    private void delete1(CommandList commandList, CallbackInfo ci) {
        for (var storage : vibrancy$sectionOverlayRenderData.values()) {
            storage.delete();
        }

        vibrancy$sectionOverlayRenderData.clear();
    }

    @Inject(
            method = "onGeometryBufferChange",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;values()Ljava/util/Collection;"
            )
    )
    private void onGeometryBufferChange(CommandList commandList, CallbackInfo ci) {
        for (var storage : vibrancy$sectionOverlayRenderData.values()) {
            storage.onBufferResized();
        }
    }

    @Inject(
            method = "onIndexBufferChange",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private void onIndexBufferChange(CommandList commandList, CallbackInfo ci) {
        for (var storage : vibrancy$sectionOverlayRenderData.values()) {
            storage.onBufferResized();
        }
    }

    @Inject(
            method = "onGeometrySegmentChange",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;onVertexSegmentChanged(I)V"
            )
    )
    private void onGeometrySegmentChange(
            CommandList commandList,
            int ownerIndex,
            CallbackInfo ci,
            @Local(name = "sectionIndex") int sectionIndex,
            @Local(name = "passIndex") int passIndex
    ) {
        vibrancy$sectionOverlayRenderData.get(DefaultTerrainRenderPasses.ALL[passIndex]).onVertexSegmentChanged(sectionIndex);
    }

    @Inject(
            method = "onIndexSegmentChange",
            at = @At("HEAD")
    )
    private void onIndexSegmentChange(
            CommandList commandList,
            int ownerIndex,
            CallbackInfo ci
    ) {
        var sectionIndex = RenderRegion.unpackSectionIndex(ownerIndex);
        var passIndex = RenderRegion.unpackPassIndex(ownerIndex);
        System.out.println("Index segment changing for " + passIndex);

        for (var storage : vibrancy$sectionOverlayRenderData.values()) { // TODO do for all or just one?
            storage.onIndexSegmentChanged(sectionIndex); // TODO what about shared?
        }

        // TODO what about clearing batches?
    }

    @Inject(
            method = "removeSection",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;values()Ljava/util/Collection;"
            )
    )
    private void removeSection(
            RenderSection section,
            CallbackInfo ci,
            @Local(name = "sectionIndex") int sectionIndex
    ) {
        for (var storage : vibrancy$sectionOverlayRenderData.values()) {
            storage.removeData(sectionIndex);
        }
    }

    @Inject(
            method = "delete",
            at = @At("TAIL")
    )
    private void delete2(CommandList commandList, CallbackInfo ci) {
        for (var batch : vibrancy$cachedOverlayBatches.values()) {
            batch.delete();
        }

        vibrancy$cachedOverlayBatches.clear();
    }

    @Inject(
            method = "clearAllCachedBatches",
            at = @At("TAIL")
    )
    private void clearAllCachedBatches(CallbackInfo ci) {
        for (var batch : vibrancy$cachedOverlayBatches.values()) {
            batch.clear();
        }
    }

    @Inject(
            method = "clearCachedBatchFor",
            at = @At("TAIL")
    )
    private void clearCachedBatchFor(TerrainRenderPass pass, CallbackInfo ci) {
        var batch = vibrancy$cachedOverlayBatches.get(pass);

        if (batch != null) {
            batch.clear();
        }
    }
}
