package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static org.lwjgl.opengl.GL30.*;

@Mixin(value = AdvancedFbo.Builder.class, remap = false)
public class AdvancedFboBuilderMixin {
    @Shadow
    private int format;

    @ModifyArg(
            method = "setDepthTextureBuffer(III)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFboTextureAttachment;<init>(IIIIIIIZIILjava/lang/String;)V"
            ),
            index = 0
    )
    private int setDepthTextureBufferAttachment(int type) {
        return format == GL_DEPTH_STENCIL ? GL_DEPTH_STENCIL_ATTACHMENT : type;
    }

    @ModifyArg(
            method = "setDepthTextureBuffer(III)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFboTextureAttachment;<init>(IIIIIIIZIILjava/lang/String;)V"
            ),
            index = 1
    )
    private int setDepthTextureBufferFormat(int format) {
        return this.format == GL_DEPTH_STENCIL ? GL_DEPTH24_STENCIL8 : format;
    }

    @ModifyArg(
            method = "setDepthRenderBuffer(II)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFboRenderAttachment;<init>(IIIII)V"
            ),
            index = 0
    )
    private int setDepthRenderBufferAttachment(int type) {
        return format == GL_DEPTH_STENCIL ? GL_DEPTH_STENCIL_ATTACHMENT : type;
    }

    @ModifyArg(
            method = "setDepthRenderBuffer(II)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFboRenderAttachment;<init>(IIIII)V"
            ),
            index = 1
    )
    private int setDepthRenderBufferFormat(int format) {
        return this.format == GL_DEPTH_STENCIL ? GL_DEPTH24_STENCIL8 : format;
    }
}
