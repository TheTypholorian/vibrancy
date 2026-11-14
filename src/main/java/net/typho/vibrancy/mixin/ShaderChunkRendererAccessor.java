package net.typho.vibrancy.mixin;

import net.caffeinemc.mods.sodium.client.gl.shader.ShaderConstants;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ShaderChunkRenderer.class)
public interface ShaderChunkRendererAccessor {
    @Invoker("createShaderConstants")
    static ShaderConstants vibrancy$createShaderConstants(ChunkShaderOptions options) {
        throw new AssertionError();
    }
}
