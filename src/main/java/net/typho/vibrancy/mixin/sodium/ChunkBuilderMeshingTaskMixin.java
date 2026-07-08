package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.big_shot_lib.api.math.IRect3;
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
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderCache;getWorldSlice()Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;"
            )
    )
    private void executeStart(
            ChunkBuildContext buildContext,
            CancellationToken cancellationToken,
            CallbackInfoReturnable<ChunkBuildOutput> cir,
            @Local ChunkBuildBuffers buffers
    ) {
        //if (Vibrancy.lightManager.blockLights.values().stream().anyMatch(storage -> storage.shouldCollectMeshGeometry(renderContext.getOrigin()))) {
        SectionMeshCache cache = SectionMeshCache.poll(renderContext.getOrigin());
        ((SectionMeshCache.Holder) buffers).setVibrancy$sectionMeshCache(cache);

        for (int x = renderContext.getOrigin().minBlockX(); x < renderContext.getOrigin().maxBlockX(); x++) {
            for (int y = renderContext.getOrigin().minBlockY(); y < renderContext.getOrigin().maxBlockY(); y++) {
                for (int z = renderContext.getOrigin().minBlockZ(); z < renderContext.getOrigin().maxBlockZ(); z++) {
                    BlockState state = buildContext.cache.getWorldSlice().getBlockState(x, y, z);
                    int index = cache.index(x, y, z) << 1;
                    cache.stateFlags.set(index, state.isAir());
                    cache.stateFlags.set(index + 1, state.isSolidRender());
                }
            }
        }

        //}
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

                Vibrancy.lightManager.nextDirtySections.add(cache.pos);
            }
        }

        holder.setVibrancy$sectionMeshCache(null);
    }
}
