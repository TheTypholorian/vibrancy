package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.core.Vec3i;
import net.typho.vibrancy.ext.ChunkBuildBuffersExt;
import net.typho.vibrancy.ext.BakedChunkModelBuilderExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChunkBuildBuffers.class)
public class ChunkBuildBuffersMixin implements ChunkBuildBuffersExt {
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

    @Inject(
            method = "createMesh",
            at = @At("HEAD")
    )
    private void createMesh(
            TerrainRenderPass pass,
            int visibleSlices,
            boolean forceUnassigned,
            boolean sliceReordering,
            CallbackInfoReturnable<BuiltSectionMeshParts> cir
    ) {
    }
}
