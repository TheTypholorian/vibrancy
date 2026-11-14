package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.core.Vec3i;
import net.typho.vibrancy.ext.BuiltSectionMeshPartsExt;
import net.typho.vibrancy.ext.ChunkBuildBuffersExt;
import net.typho.vibrancy.ext.BakedChunkModelBuilderExt;
import net.typho.vibrancy.ext.ChunkMeshBufferBuilderExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChunkBuildBuffers.class)
public class ChunkBuildBuffersMixin implements ChunkBuildBuffersExt {
    @Shadow
    public static int[] makeVertexSegments() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Unique
    private final List<Vec3i> vibrancy$lightSources = new ArrayList<>();

    @ModifyArgs(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/Reference2ReferenceOpenHashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private void init(Args args) {
        args.<BakedChunkModelBuilderExt>get(1).vibrancy$setInfo((ChunkBuildBuffers) (Object) this, args.get(0));
    }

    @Override
    public List<Vec3i> vibrancy$getLightSources() {
        return vibrancy$lightSources;
    }

    @Inject(
            method = "init",
            at = @At("TAIL")
    )
    private void init(BuiltSectionInfo.Builder renderData, int sectionIndex, CallbackInfo ci) {
        vibrancy$lightSources.clear();
    }

    @ModifyReturnValue(
            method = "createMesh",
            at = @At("RETURN")
    )
    private BuiltSectionMeshParts createMesh(
            BuiltSectionMeshParts mesh,
            @Local(name = "builder") BakedChunkModelBuilder builder
    ) {
        var indexTotal = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            indexTotal += ((ChunkMeshBufferBuilderExt) builder.getVertexBuffer(facing)).vibrancy$overlayIndicesCount();
        }

        if (indexTotal != 0) {
            var ext = (BuiltSectionMeshPartsExt) mesh;
            var mergedBuffer = new NativeBuffer(indexTotal * 4);
            var mergedBufferBuilder = mergedBuffer.getDirectBuffer();

            for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
                var buffer = ((ChunkMeshBufferBuilderExt) builder.getVertexBuffer(facing));

                if (buffer.vibrancy$overlayIndicesCount() != 0) {
                    mergedBufferBuilder.put(buffer.vibrancy$sliceOverlayIndices());
                }
            }

            ext.vibrancy$setOverlayIndexBuffer(mergedBuffer);
        }

        return mesh;
    }
}
