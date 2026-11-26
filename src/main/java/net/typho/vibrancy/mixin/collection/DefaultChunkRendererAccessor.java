package net.typho.vibrancy.mixin.collection;

import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DefaultChunkRenderer.class)
public interface DefaultChunkRendererAccessor {
    @Invoker("addSharedIndexedDrawCommands")
    static void vibrancy$addSharedIndexedDrawCommands(MultiDrawBatch batch, long pMeshData, int mask) {
    }
}
