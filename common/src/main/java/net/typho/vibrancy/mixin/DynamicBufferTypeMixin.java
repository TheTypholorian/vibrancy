package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.glsl.grammar.GlslTypeSpecifier;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DynamicBufferType.class, remap = false)
public class DynamicBufferTypeMixin {
    @Shadow
    @Final
    @Mutable
    private static DynamicBufferType[] $VALUES;

    @Shadow
    @Final
    @Mutable
    private String sourceName;

    @Shadow
    @Final
    @Mutable
    private static DynamicBufferType[] BUFFERS;

    @Invoker("<init>")
    public static DynamicBufferType init(String name, int ordinal, String sourceName, GlslTypeSpecifier.BuiltinType type, FramebufferAttachmentDefinition.Format format) {
        throw new IllegalStateException();
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void init(String name, int ordinal, String sourceName, GlslTypeSpecifier.BuiltinType type, FramebufferAttachmentDefinition.Format format, CallbackInfo ci) {
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
        Vibrancy.POSITION_BUFFER_TYPE = array[$VALUES.length] = init("VIBRANCY_POSITION", $VALUES.length, "VibrancyPosition", GlslTypeSpecifier.BuiltinType.VEC3, FramebufferAttachmentDefinition.Format.RGB16F);
        BUFFERS = $VALUES = array;
    }
}
