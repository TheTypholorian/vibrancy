package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.typho.vibrancy.light.BlockLight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public class ClientChunkCacheStorageMixin {
    @WrapOperation(
            method = "replace(ILnet/minecraft/world/level/chunk/LevelChunk;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/atomic/AtomicReferenceArray;getAndSet(ILjava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private <E> E replace(AtomicReferenceArray<E> instance, int i, E newValue, Operation<E> original) {
        E chunk = original.call(instance, i, newValue);

        if (newValue == null) {
            if (chunk instanceof LevelChunk levelChunk) {
                BlockLight.clearChunk(levelChunk);
            }
        } else if (newValue instanceof LevelChunk levelChunk) {
            BlockLight.scanChunk(levelChunk);
        }

        return chunk;
    }

    @WrapOperation(
            method = "replace(ILnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/atomic/AtomicReferenceArray;compareAndSet(ILjava/lang/Object;Ljava/lang/Object;)Z"
            )
    )
    private <E> boolean replace(AtomicReferenceArray<E> instance, int i, E expectedValue, E newValue, Operation<Boolean> original) {
        boolean r = original.call(instance, i, expectedValue, newValue);

        if (r) {
            if (newValue instanceof LevelChunk levelChunk) {
                BlockLight.scanChunk(levelChunk);
            } else if (expectedValue instanceof LevelChunk levelChunk) {
                BlockLight.clearChunk(levelChunk);
            }
        }

        return r;
    }
}
