package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.caffeinemc.mods.sodium.client.gl.arena.BufferSegment;
import net.caffeinemc.mods.sodium.client.gl.arena.PendingUpload;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.typho.vibrancy.ext.BuiltSectionMeshPartsExt;
import net.typho.vibrancy.ext.RenderRegionExt;
import net.typho.vibrancy.mirror.PendingSectionOverlayIndexBufferUpload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(RenderRegionManager.class)
public class RenderRegionManagerMixin {
    @Inject(
            method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;)V",
            at = @At("HEAD")
    )
    private void uploadResults1(
            CommandList commandList,
            RenderRegion region,
            Collection<BuilderTaskOutput> results,
            UniformBufferManager uniforms,
            CallbackInfo ci,
            @Share("overlayIndexUploads") LocalRef<List<PendingSectionOverlayIndexBufferUpload>> overlayIndexUploads
    ) {
        overlayIndexUploads.set(new ArrayList<>());
    }

    @Inject(
            method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z",
                    ordinal = 0
            )
    )
    private void uploadResults2(
            CommandList commandList,
            RenderRegion region,
            Collection<BuilderTaskOutput> results,
            UniformBufferManager uniforms,
            CallbackInfo ci,
            @Share("overlayIndexUploads") LocalRef<List<PendingSectionOverlayIndexBufferUpload>> overlayIndexUploads,
            @Local(name = "result") BuilderTaskOutput result,
            @Local(name = "renderSectionIndex") int renderSectionIndex,
            @Local(name = "passIndex") int passIndex,
            @Local(name = "pass") TerrainRenderPass pass,
            @Local(name = "mesh") BuiltSectionMeshParts mesh
    ) {
        var indexBuffer = ((BuiltSectionMeshPartsExt) mesh).vibrancy$getOverlayIndexBuffer();

        if (indexBuffer != null) {
            overlayIndexUploads.get().add(new PendingSectionOverlayIndexBufferUpload(result.section, pass, new PendingUpload(indexBuffer, RenderRegion.packOwnerIndex(renderSectionIndex, passIndex))));
        }
    }

    @WrapOperation(
            method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;isEmpty()Z",
                    ordinal = 1
            )
    )
    private boolean uploadResults3(
            ArrayList<Object> instance,
            Operation<Boolean> original,
            @Share("overlayIndexUploads") LocalRef<List<PendingSectionOverlayIndexBufferUpload>> overlayIndexUploads
    ) {
        return original.call(instance) && overlayIndexUploads.get().isEmpty();
    }

    @Inject(
            method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;isEmpty()Z",
                    ordinal = 3
            )
    )
    private void uploadResults4(
            CommandList commandList,
            RenderRegion region,
            Collection<BuilderTaskOutput> results,
            UniformBufferManager uniforms,
            CallbackInfo ci,
            @Share("overlayIndexUploads") LocalRef<List<PendingSectionOverlayIndexBufferUpload>> overlayIndexUploads,
            @Local(name = "resources") RenderRegion.DeviceResources resources,
            @Local(name = "indexBufferChanged") LocalBooleanRef indexBufferChanged
    ) {
        var uploads = overlayIndexUploads.get();

        if (!uploads.isEmpty()) {
            var allocator = resources.getIndexAllocator();
            var result = allocator.upload(commandList, uploads.stream().map(PendingSectionOverlayIndexBufferUpload::indexBufferUpload));
            indexBufferChanged.set(indexBufferChanged.get() || result);

            for (var upload : uploads) {
                var storage = ((RenderRegionExt) region).vibrancy$getOrCreateOverlayStorage(upload.pass());
                storage.setIndexData(upload.section().getSectionIndex(), upload.indexBufferUpload().getResult());
            }
        }
    }

    @WrapOperation(
            method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;createStorage(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;",
                    ordinal = 0
            )
    )
    private SectionRenderDataStorage uploadResults5(
            RenderRegion instance,
            TerrainRenderPass pass,
            Operation<SectionRenderDataStorage> original,
            @Share("uploadPass") LocalRef<TerrainRenderPass> uploadPass
    ) {
        uploadPass.set(pass);
        return original.call(instance, pass);
    }

    @WrapOperation(
            method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;Lnet/caffeinemc/mods/sodium/client/render/chunk/UniformBufferManager;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataStorage;setVertexData(ILnet/caffeinemc/mods/sodium/client/gl/arena/BufferSegment;[I)V"
            )
    )
    private void uploadResults6(
            SectionRenderDataStorage instance,
            int localSectionIndex,
            BufferSegment allocation,
            int[] vertexSegments,
            Operation<Void> original,
            @Local(argsOnly = true) RenderRegion region,
            @Share("uploadPass") LocalRef<TerrainRenderPass> uploadPass
    ) {
        original.call(instance, localSectionIndex, allocation, vertexSegments);
        original.call(((RenderRegionExt) region).vibrancy$getOrCreateOverlayStorage(uploadPass.get()), localSectionIndex, allocation, vertexSegments);
    }
}
