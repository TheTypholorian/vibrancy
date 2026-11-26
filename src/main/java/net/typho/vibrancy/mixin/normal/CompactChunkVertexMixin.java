package net.typho.vibrancy.mixin.normal;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.ext.ChunkVertexExt;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Debug(export = true)
@Mixin(CompactChunkVertex.class)
public class CompactChunkVertexMixin {
    @Inject(
            method = "lambda$getEncoder$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/api/memory/MemoryIntrinsics;putInt(JI)V",
                    ordinal = 4,
                    shift = At.Shift.AFTER
            )
    )
    private static void setNormal(
            long ptr,
            int materialBits,
            ChunkVertexEncoder.Vertex[] vertices,
            int section,
            CallbackInfoReturnable<Long> cir,
            @Local ChunkVertexEncoder.Vertex vertex
    ) {
        MemoryIntrinsics.putInt(ptr + 20L, ((ChunkVertexExt) vertex).vibrancy$getNormal());
    }

    @ModifyConstant(
            method = "lambda$getEncoder$0",
            constant = @Constant(longValue = 20L)
    )
    private static long vertexStride(long constant) {
        return constant + 4L;
    }

    @ModifyConstant(
            method = "<clinit>",
            constant = @Constant(intValue = 20)
    )
    private static int vertexStride(int constant) {
        return constant + 4;
    }

    @WrapOperation(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/gl/attribute/GlVertexFormat$Builder;build()Lnet/caffeinemc/mods/sodium/client/gl/attribute/GlVertexFormat;"
            )
    )
    private static GlVertexFormat buildFormat(GlVertexFormat.Builder instance, Operation<GlVertexFormat> original) {
        return original.call(instance.addElement(Vibrancy.normalVertexAttribute, 4, 20));
    }
}
