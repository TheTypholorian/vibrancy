package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.vibrancy.VibrancyChunkMeshingTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkBuilderMeshingTask.class)
public class ChunkBuilderMeshingTaskMixin {
    @WrapOperation(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"
            )
    )
    private boolean execute(
            BlockState state,
            Operation<Boolean> original,
            @Local(name = "x") int x,
            @Local(name = "y") int y,
            @Local(name = "z") int z
    ) {
        if ((Object) this instanceof VibrancyChunkMeshingTask vibrancy) {
            for (BlockPos light : vibrancy.lights) {
                if (light.distSqr(new Vec3i(x & 15, y & 15, z & 15)) < (6 * 6)) {
                    return original.call(state);
                }
            }

            return true;
        }

        return original.call(state);
    }
}
