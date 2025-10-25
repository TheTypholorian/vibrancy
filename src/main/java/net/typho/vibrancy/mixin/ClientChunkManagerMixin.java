package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ClientChunkManager.class)
public class ClientChunkManagerMixin {
    @Inject(
            method = "loadChunkFromPacket",
            at = @At("TAIL")
    )
    private void onChunkLoad(int x, int z, PacketByteBuf packetByteBuf, NbtCompound nbtCompound, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfoReturnable<WorldChunk> info) {
        Vibrancy.onChunkLoad(info.getReturnValue());
    }

    @Inject(
            method = "loadChunkFromPacket",
            at = @At(
                    value = "NEW",
                    target = "net/minecraft/world/chunk/WorldChunk",
                    shift = At.Shift.BEFORE
            )
    )
    private void onChunkUnload(int x, int z, PacketByteBuf buf, NbtCompound tag, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfoReturnable<WorldChunk> info, @Local WorldChunk chunk) {
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
