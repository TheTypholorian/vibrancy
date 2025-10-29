package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.glsl.grammar.GlslTypeSpecifier;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicBufferType.class)
public class DynamicBufferTypeMixin {
    @Shadow
    @Final
    @Mutable
    private static DynamicBufferType[] $VALUES;

    @Shadow
    @Final
    @Mutable
    private String sourceName;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void init(String sourceName, int type, String format, GlslTypeSpecifier.BuiltinType par4, FramebufferAttachmentDefinition.Format par5, CallbackInfo ci) {
        if (sourceName.equals("VibrancyPosition")) {
            this.sourceName = "VibrancyDynamicPosition";
        }
    }

    @Inject(
            method = "<clinit>",
            at = @At("TAIL")
    )
    private static void clinit(CallbackInfo ci) {
        DynamicBufferType[] array = new DynamicBufferType[$VALUES.length + 1];
        System.arraycopy($VALUES, 0, array, 0, $VALUES.length);
        Vibrancy.POSITION_BUFFER_TYPE = array[$VALUES.length] = new DynamicBufferType("VibrancyPosition", GlslTypeSpecifier.BuiltinType.VEC3, FramebufferAttachmentDefinition.Format.RGB16F);
        $VALUES = array;
    }
}
