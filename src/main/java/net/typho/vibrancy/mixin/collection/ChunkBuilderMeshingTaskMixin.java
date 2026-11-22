package net.typho.vibrancy.mixin.collection;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.collection.VibrancyChunkMeshingTask;
import net.typho.vibrancy.collection.VibrancyChunkVertexType;
import net.typho.vibrancy.storage.BlockLight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
            if (vibrancy.light.distSqr(new Vec3i(x, y, z)) > (BlockLight.LIGHT_RADIUS * BlockLight.LIGHT_RADIUS)) {
                return true;
            } else if (vibrancy.light.getX() == x && vibrancy.light.getY() == y && vibrancy.light.getZ() == z) {
                return true; // TODO
            }
        }

        return original.call(state);
    }

    @Inject(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;<init>()V"
            )
    )
    private void execute1(
            ChunkBuildContext buildContext,
            CancellationToken cancellationToken,
            CallbackInfoReturnable<ChunkBuildOutput> cir,
            @Local(name = "blockPos") BlockPos.MutableBlockPos blockPos
    ) {
        var vertexType = ((ChunkBuildBuffersAccessor) buildContext.buffers).vibrancy$vertexType();

        if (vertexType instanceof VibrancyChunkVertexType vibrancy) {
            vibrancy.blockPos = blockPos;
        }
    }
}
