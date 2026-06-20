package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.util.SectionMeshCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkBuilderMeshingTask.class)
public class ChunkBuilderMeshingTaskMixin {
    @Shadow
    @Final
    private ChunkRenderContext renderContext;

    @Inject(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;init(Lnet/caffeinemc/mods/sodium/client/render/chunk/data/BuiltSectionInfo$Builder;I)V"
            )
    )
    private void executeStart(
            ChunkBuildContext buildContext,
            CancellationToken cancellationToken,
            CallbackInfoReturnable<ChunkBuildOutput> cir,
            @Local ChunkBuildBuffers buffers
    ) {
        if (Vibrancy.lightManager.blockLights.values().stream().anyMatch(storage -> storage.shouldCollectMeshGeometry(renderContext.getOrigin()))) {
            ((SectionMeshCache.Holder) buffers).setVibrancy$sectionMeshCache(SectionMeshCache.poll(renderContext.getOrigin()));
        }
    }

    @Inject(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At("RETURN")
    )
    private void executeEnd(
            ChunkBuildContext buildContext,
            CancellationToken cancellationToken,
            CallbackInfoReturnable<ChunkBuildOutput> cir,
            @Local ChunkBuildBuffers buffers
    ) {
        var holder = (SectionMeshCache.Holder) buffers;
        var cache = holder.getVibrancy$sectionMeshCache();

        if (cache != null) {
            synchronized (Vibrancy.lightManager.sectionLock) {
                SectionMeshCache old = Vibrancy.lightManager.sectionMeshCaches.put(cache.pos, cache);

                if (old != null) {
                    SectionMeshCache.getPool().add(old);
                }

                Vibrancy.lightManager.nextDirtySections.add(new Pair<>(cache.pos, new IRect3(cache.pos.minBlockX(), cache.pos.minBlockY(), cache.pos.minBlockZ(), cache.pos.maxBlockX(), cache.pos.maxBlockY(), cache.pos.maxBlockZ())));
            }
        }

        holder.setVibrancy$sectionMeshCache(null);
    }
}
