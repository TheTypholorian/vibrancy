package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.typho.big_shot_lib.api.client.rendering.util.mesh.EmptyVertexConsumer;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.entity.StagedVertexBufferDrawExtension;
import net.typho.vibrancy.entity.VibrancyEntityShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StagedVertexBuffer.class)
public class StagedVertexBufferMixin {
    @ModifyArg(
            method = "appendDraw(Lcom/mojang/blaze3d/vertex/VertexFormat;Lcom/mojang/blaze3d/PrimitiveTopology;Lcom/mojang/blaze3d/vertex/VertexSorting;)Lnet/minecraft/client/renderer/StagedVertexBuffer$Draw;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/StagedVertexBuffer$Draw;<init>(Lcom/mojang/blaze3d/vertex/VertexFormat;Lcom/mojang/blaze3d/PrimitiveTopology;Lcom/mojang/blaze3d/vertex/VertexSorting;)V"
            )
    )
    private VertexFormat appendDraw(
            VertexFormat format,
            @Share("originalFormat") LocalRef<VertexFormat> originalFormat
    ) {
        if ((Object) this instanceof VibrancyEntityShadowFeatureRenderer.VertexBuffer) {
            originalFormat.set(format);
            return Vibrancy.entityShadowFormat;
        } else {
            originalFormat.set(null);
        }

        return format;
    }

    @ModifyReturnValue(
            method = "appendDraw(Lcom/mojang/blaze3d/vertex/VertexFormat;Lcom/mojang/blaze3d/PrimitiveTopology;Lcom/mojang/blaze3d/vertex/VertexSorting;)Lnet/minecraft/client/renderer/StagedVertexBuffer$Draw;",
            at = @At("TAIL")
    )
    private StagedVertexBuffer.Draw appendDraw(
            StagedVertexBuffer.Draw original,
            @Share("originalFormat") LocalRef<VertexFormat> originalFormat
    ) {
        if (originalFormat.get() != null) {
            ((StagedVertexBufferDrawExtension) original).setVibrancy$originalFormat(originalFormat.get());
        }

        return original;
    }

    @WrapOperation(
            method = "getVertexBuilder",
            at = @At(
                    value = "NEW",
                    target = "(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;Lcom/mojang/blaze3d/PrimitiveTopology;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"
            )
    )
    private BufferBuilder getVertexBuilder(
            ByteBufferBuilder byteBufferBuilder,
            PrimitiveTopology primitiveTopology,
            VertexFormat vertexFormat,
            Operation<BufferBuilder> original,
            @Local(argsOnly = true) StagedVertexBuffer.Draw draw,
            @Share("insufficientComponents") LocalBooleanRef insufficientComponents
    ) {
        if ((Object) this instanceof VibrancyEntityShadowFeatureRenderer.VertexBuffer) {
            VertexFormat originalFormat = ((StagedVertexBufferDrawExtension) draw).getVibrancy$originalFormat();

            if (
                    originalFormat == null ||
                            !originalFormat.contains(DefaultVertexFormat.POSITION_SEMANTIC_NAME) ||
                            !originalFormat.contains(DefaultVertexFormat.UV0_SEMANTIC_NAME) ||
                            !originalFormat.contains(DefaultVertexFormat.COLOR_SEMANTIC_NAME)
            ) {
                insufficientComponents.set(true);
                return null;
            }

            insufficientComponents.set(false);

            return original.call(byteBufferBuilder, primitiveTopology, Vibrancy.entityShadowFormat);
        } else {
            return original.call(byteBufferBuilder, primitiveTopology, vertexFormat);
        }
    }

    @WrapOperation(
            method = "getVertexBuilder",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Objects;requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    @SuppressWarnings("unchecked")
    private <T> T getVertexBuilder(
            T obj,
            Operation<T> original
    ) {
        if ((Object) this instanceof VibrancyEntityShadowFeatureRenderer.VertexBuffer) {
            return obj == null ? (T) EmptyVertexConsumer.INSTANCE : obj;
        } else {
            return original.call(obj);
        }
    }

    @ModifyReturnValue(
            method = "getVertexBuilder",
            at = @At(
                    value = "RETURN",
                    ordinal = 1
            )
    )
    private VertexConsumer getVertexBuilder(
            VertexConsumer original,
            @Share("insufficientComponents") LocalBooleanRef insufficientComponents
    ) {
        return insufficientComponents.get() ? EmptyVertexConsumer.INSTANCE : original;
    }
}
