package net.typho.vibrancy.mixin.sodium.normal;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.typho.vibrancy.sodium.SodiumChunkVertexExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CompactChunkVertex.class)
public class CompactChunkVertexMixin {
    @Definition(id = "ptr", local = @Local(type = long.class))
    @Expression("ptr + 20")
    @Inject(
            method = "lambda$getEncoder$0",
            at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private static void encode(
            long ptr,
            int materialBits,
            ChunkVertexEncoder.Vertex[] vertices,
            int section,
            CallbackInfoReturnable<Long> cir,
            @Local ChunkVertexEncoder.Vertex vertex
    ) {
        MemoryIntrinsics.putInt(ptr + 20L, ((SodiumChunkVertexExtension) vertex).getVibrancy$normal());
    }

    @ModifyConstant(
            method = "lambda$getEncoder$0",
            constant = @Constant(longValue = 20)
    )
    private static long encoderStride(long constant) {
        return constant + 4;
    }

    /*
    @ModifyConstant(
            method = "<clinit>",
            constant = @Constant(intValue = 20)
    )
    private static int formatStride(int constant) {
        return constant + 4;
    }
     */

    @WrapOperation(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexFormat$Builder;build()Lcom/mojang/blaze3d/vertex/VertexFormat;"
            )
    )
    private static VertexFormat build(
            VertexFormat.Builder instance,
            Operation<VertexFormat> original
    ) {
        return original.call(instance.addAttribute("a_VibrancyNormal", 4, GpuFormat.RGB8_SNORM));
    }
}
