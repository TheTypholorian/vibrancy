package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.textures.GpuSamplerImpl;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.RenderSetup;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.typho.vibrancy.entity.PreparedRenderTypeTextureExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(RenderSetup.class)
public class RenderSetupMixin {
    @WrapOperation(
            method = "prepareTextures",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/lang/String;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSamplerImpl;)Lnet/minecraft/client/renderer/rendertype/PreparedRenderType$Texture;",
                    ordinal = 2
            )
    )
    private PreparedRenderType.Texture prepareTextures(
            String name,
            GpuTextureView textureView,
            GpuSamplerImpl sampler,
            Operation<PreparedRenderType.Texture> original,
            @Local Map.Entry<String, RenderSetup.TextureBinding> entry
    ) {
        PreparedRenderType.Texture texture = original.call(name, textureView, sampler);
        ((PreparedRenderTypeTextureExtension) (Object) texture).setVibrancy$identifier(entry.getValue().location());
        return texture;
    }
}
