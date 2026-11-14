package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.gl.shader.*;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface;
import net.minecraft.resources.Identifier;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.VibrancyRenderPasses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderChunkRenderer.class)
public class ShaderChunkRendererMixin {
    @Shadow
    private static ShaderConstants createShaderConstants(ChunkShaderOptions options) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @WrapOperation(
            method = "compileProgram",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/ShaderChunkRenderer;createShader(Ljava/lang/String;Lnet/caffeinemc/mods/sodium/client/render/chunk/shader/ChunkShaderOptions;)Lnet/caffeinemc/mods/sodium/client/gl/shader/GlProgram;"
            )
    )
    private GlProgram<ChunkShaderInterface> compileProgram(
            ShaderChunkRenderer instance,
            String path,
            ChunkShaderOptions options,
            Operation<GlProgram<ChunkShaderInterface>> original
    ) {
        if (options.pass() == VibrancyRenderPasses.SOLID || options.pass() == VibrancyRenderPasses.CUTOUT || options.pass() == VibrancyRenderPasses.TRANSLUCENT) {
            ShaderConstants constants = createShaderConstants(options);

            GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX, Vibrancy.id(path + ".vsh"), constants);
            GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, Vibrancy.id(path + ".fsh"), constants);

            try {
                return GlProgram.builder(Vibrancy.id("chunk_shader"))
                        .attachShader(vertShader)
                        .attachShader(fragShader)
                        .bindAttribute("a_Position", ChunkShaderBindingPoints.ATTRIBUTE_POSITION)
                        .bindAttribute("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                        .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_TEXTURE)
                        .bindAttribute("a_LightAndData", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_MATERIAL_INDEX)
                        .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                        .link((shader) -> new DefaultShaderInterface(shader, options));
            } finally {
                vertShader.delete();
                fragShader.delete();
            }
        } else {
            return original.call(instance, path, options);
        }
    }
}
