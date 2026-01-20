package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatAttribute;
import net.typho.vibrancy.sodium.VertexWithNormal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.system.MemoryUtil.memPutInt;

@Mixin(value = CompactChunkVertex.class, remap = false)
public class CompactChunkVertexMixin {
    @Inject(
            method = "lambda$getEncoder$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/system/MemoryUtil;memPutInt(JI)V",
                    ordinal = 4,
                    shift = At.Shift.AFTER
            )
    )
    private static void storeNormal(
            long ptr,
            int materialBits,
            ChunkVertexEncoder.Vertex[] vertices,
            int section,
            CallbackInfoReturnable<Long> cir,
            @Local ChunkVertexEncoder.Vertex vertex
    ) {
        memPutInt(ptr + 20L, ((VertexWithNormal) vertex).vibrancy$getNormal());
    }

    @ModifyConstant(
            method = "lambda$getEncoder$0",
            constant = @Constant(longValue = 20)
    )
    private static long encoderStride(long constant) {
        return constant + 4;
    }

    @ModifyConstant(
            method = "<clinit>",
            constant = @Constant(intValue = 20)
    )
    private static int formatStride(int constant) {
        return constant + 4;
    }

    @WrapOperation(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/gl/attribute/GlVertexFormat$Builder;build()Lnet/caffeinemc/mods/sodium/client/gl/attribute/GlVertexFormat;"
            )
    )
    private static GlVertexFormat clinit(GlVertexFormat.Builder instance, Operation<GlVertexFormat> original) {
        return original.call(instance.addElement(new VertexFormatAttribute("NORMAL", GlVertexAttributeFormat.BYTE, 3, true, false), 4, 20));
    }
}
