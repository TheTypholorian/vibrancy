package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.typho.vibrancy.ext.ChunkBuildBuffersExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkBuilderMeshingTask.class)
public class ChunkBuilderMeshingTaskMixin {
    @Inject(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderCache;getBlockRenderer()Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;"
            )
    )
    private void execute(
            ChunkBuildContext buildContext,
            CancellationToken cancellationToken,
            CallbackInfoReturnable<ChunkBuildOutput> cir,
            @Local(name = "buffers") ChunkBuildBuffers buffers,
            @Local(name = "slice") LevelSlice slice,
            @Local(name = "minX") int minX,
            @Local(name = "minY") int minY,
            @Local(name = "minZ") int minZ,
            @Local(name = "maxX") int maxX,
            @Local(name = "maxY") int maxY,
            @Local(name = "maxZ") int maxZ
    ) {
        for (int x = minX - 8; x < maxX + 8; x++) {
            for (int y = minY - 8; y < maxY + 8; y++) {
                for (int z = minZ - 8; z < maxZ + 8; z++) {
                    var state = slice.getBlockState(x, y, z); // TODO doesn't work because chunk borders

                    if (state.getLightEmission() > 0) {
                        ((ChunkBuildBuffersExt) buffers).vibrancy$getLightSources().add(new Vec3i(x - minX, y - minY, z - minZ));
                    }
                }
            }
        }
    }
}
