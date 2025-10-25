package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {
    @Inject(
            method = "replaceWithPacketData",
            at = @At("TAIL")
    )
    private void onChunkLoad(int x, int z, FriendlyByteBuf packetByteBuf, CompoundTag nbtCompound, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> info) {
        Vibrancy.onChunkLoad(info.getReturnValue());
    }

    @Inject(
            method = "replaceWithPacketData",
            at = @At(
                    value = "NEW",
                    target = "net/minecraft/world/chunk/WorldChunk",
                    shift = At.Shift.BEFORE
            )
    )
    private void onChunkLoad(int x, int z, FriendlyByteBuf packetByteBuf, CompoundTag nbtCompound, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> info, @Local LevelChunk chunk) {
        if (chunk != null) {
            Vibrancy.onChunkUnload(chunk);
        }
    }

    @Inject(
            method = "unload",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;compareAndSet(ILnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/WorldChunk;)Lnet/minecraft/world/chunk/WorldChunk;"
            )
    )
    private void onChunkUnload(ChunkPos pos, CallbackInfo ci, @Local WorldChunk chunk) {
        Vibrancy.onChunkUnload(chunk);
    }

    @Inject(
            method = "updateLoadDistance",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/world/ClientChunkManager$ClientChunkMap.isInRadius(II)Z"
            )
    )
    private void onUpdateLoadDistance(int loadDistance, CallbackInfo ci, @Local ClientChunkManager.ClientChunkMap clientChunkMap, @Local WorldChunk chunk, @Local ChunkPos chunkPos) {
        if (!clientChunkMap.isInRadius(chunkPos.x, chunkPos.z)) {
            Vibrancy.onChunkUnload(chunk);
        }
    }
}
