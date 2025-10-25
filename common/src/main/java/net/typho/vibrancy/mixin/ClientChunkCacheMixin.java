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
                    target = "net/minecraft/world/level/chunk/LevelChunk",
                    shift = At.Shift.BEFORE
            )
    )
    private void onChunkLoad(int x, int z, FriendlyByteBuf packetByteBuf, CompoundTag nbtCompound, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> info, @Local LevelChunk chunk) {
        if (chunk != null) {
            Vibrancy.onChunkUnload(chunk);
        }
    }

    @Inject(
            method = "drop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;replace(ILnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)Lnet/minecraft/world/level/chunk/LevelChunk;"
            )
    )
    private void onChunkUnload(ChunkPos pos, CallbackInfo ci, @Local LevelChunk chunk) {
        Vibrancy.onChunkUnload(chunk);
    }
}
