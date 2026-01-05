package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.typho.vibrancy.sodium.VeilChunkVertex;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Developers of Veil
 */
@Pseudo
@Mixin(value = ChunkMeshFormats.class, remap = false)
public class ChunkMeshFormatsMixin {
    @Mutable
    @Shadow
    @Final
    public static ChunkVertexType COMPACT;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void clinit(CallbackInfo ci) {
        COMPACT = new VeilChunkVertex();
    }
}
