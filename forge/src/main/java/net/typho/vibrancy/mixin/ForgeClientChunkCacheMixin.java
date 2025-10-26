package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkCache.class)
public class ForgeClientChunkCacheMixin {
    @Inject(
            method = "updateViewRadius",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;inRange(II)Z"
            )
    )
    private void onUpdateLoadDistance(int loadDistance, CallbackInfo ci, @Local ClientChunkCache.Storage storage, @Local LevelChunk chunk, @Local ChunkPos chunkPos) {
        if (!storage.inRange(chunkPos.x, chunkPos.z)) {
            Vibrancy.onChunkUnload(chunk);
        }
    }
}
