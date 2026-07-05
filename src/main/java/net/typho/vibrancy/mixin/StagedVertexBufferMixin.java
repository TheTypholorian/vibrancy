package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.typho.big_shot_lib.api.client.rendering.util.mesh.EmptyVertexConsumer;
import net.typho.vibrancy.entity.VibrancyEntityShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

@Mixin(StagedVertexBuffer.class)
public class StagedVertexBufferMixin {
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
            @Share("insufficientComponents") LocalBooleanRef insufficientComponents
    ) {
        if ((Object) this instanceof VibrancyEntityShadowFeatureRenderer.VertexBuffer) {
            VertexFormat newFormat = DefaultVertexFormat.POSITION_TEX_COLOR;

            if (
                    !Objects.equals(vertexFormat.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME), newFormat.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME)) ||
                    !Objects.equals(vertexFormat.getElement(DefaultVertexFormat.UV0_SEMANTIC_NAME), newFormat.getElement(DefaultVertexFormat.UV0_SEMANTIC_NAME)) ||
                    !Objects.equals(vertexFormat.getElement(DefaultVertexFormat.COLOR_SEMANTIC_NAME), newFormat.getElement(DefaultVertexFormat.COLOR_SEMANTIC_NAME))
            ) {
                insufficientComponents.set(true);
                return null;
            }

            insufficientComponents.set(false);

            return original.call(byteBufferBuilder, primitiveTopology, newFormat);
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
