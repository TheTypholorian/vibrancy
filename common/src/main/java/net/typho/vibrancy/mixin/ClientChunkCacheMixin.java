package net.typho.vibrancy.mixin;

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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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
                    target = "net/minecraft/world/level/chunk/LevelChunk",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onChunkUnload(int x, int z, FriendlyByteBuf buf, CompoundTag tag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> info, int index, LevelChunk chunk, ChunkPos chunkPos) {
        if (chunk != null) {
            Vibrancy.onChunkUnload(chunk);
        }
    }

    @Inject(
            method = "drop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;replace(ILnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)Lnet/minecraft/world/level/chunk/LevelChunk;"
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void onChunkUnload(int chunkX, int chunkZ, CallbackInfo ci, int i, LevelChunk chunk) {
        Vibrancy.onChunkUnload(chunk);
    }
}
