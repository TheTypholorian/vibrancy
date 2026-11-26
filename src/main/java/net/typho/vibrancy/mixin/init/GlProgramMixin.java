package net.typho.vibrancy.mixin.init;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import kotlin.collections.CollectionsKt;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(GlProgram.class)
public class GlProgramMixin {
    @WrapOperation(
            method = "link",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexFormat;getElementAttributeNames()Ljava/util/List;"
            )
    )
    private static List<String> link(
            VertexFormat instance,
            Operation<List<String>> original,
            @Local(argsOnly = true, ordinal = 0) GlShaderModule vertexShader,
            @Local(ordinal = 0) int programId
    ) {
        if (Vibrancy.chunkVertexShaders.contains(vertexShader.getId())) {
            GlStateManager._glBindAttribLocation(programId, ChunkShaderBindingPoints.ATTRIBUTE_POSITION, "a_Position");
            GlStateManager._glBindAttribLocation(programId, ChunkShaderBindingPoints.ATTRIBUTE_COLOR, "a_Color");
            GlStateManager._glBindAttribLocation(programId, ChunkShaderBindingPoints.ATTRIBUTE_TEXTURE, "a_TexCoord");
            GlStateManager._glBindAttribLocation(programId, ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_MATERIAL_INDEX, "a_LightAndData");

            return CollectionsKt.listOf();
        }

        return original.call(instance);
    }
}
