package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D;
import net.typho.big_shot_lib.api.math.vec.NeoVec2i;
import net.typho.big_shot_lib.api.math.vec.NeoVec3f;
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier;
import net.typho.vibrancy.block.BlockLightInfo;
import net.typho.vibrancy.block.BlockLightRegistry;
import net.typho.vibrancy.shadows.RaytracedGuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? }

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Shadow
    @Final
    private PoseStack pose;

    @Shadow
    @Final
    private Minecraft minecraft;

    //? if <1.20.5 {
    /*@Unique
    private static <T extends Comparable<T>> BlockState big_shot_lib$updateState(BlockState blockState, Property<T> property, String string) {
        return property.getValue(string).map((comparable) -> blockState.setValue(property, comparable)).orElse(blockState);
    }
    *///? }

    @WrapOperation(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"
            )
    )
    private void renderItem(
            ItemRenderer instance,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay,
            BakedModel model,
            Operation<Void> original
    ) {
        if ((Object) this instanceof RaytracedGuiGraphics raytraced) {
            if (stack.getItem() instanceof BlockItem blockItem) {
                //? if <1.20.5 {
                /*BlockState state = blockItem.getBlock().defaultBlockState();
                CompoundTag tag = stack.getTag();

                if (tag != null) {
                    CompoundTag stateTag = tag.getCompound("BlockStateTag");
                    StateDefinition<Block, BlockState> stateDefinition = blockItem.getBlock().getStateDefinition();

                    for(String string : stateTag.getAllKeys()) {
                        Property<?> property = stateDefinition.getProperty(string);
                        if (property != null) {
                            String string2 = stateTag.get(string).getAsString();
                            state = big_shot_lib$updateState(state, property, string2);
                        }
                    }
                }
                *///? } else {
                BlockState state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(blockItem.getBlock().defaultBlockState());
                //? }

                BlockLightInfo info = BlockLightRegistry.blockMap.get(blockItem.getBlock());

                if (info != null && info.getEnabled().invoke(state)) {
                    float scale = (float) minecraft.getWindow().getGuiScale();
                    Vector4f pos = pose.last().pose().transform(new Vector4f());
                    raytraced.lights.add(new RaytracedGuiGraphics.Light(new NeoVec2i((int) (pos.x * scale), (int) (pos.y * scale)), new NeoVec3f(pos.x, pos.y, pos.z), stack, state, info));

                    raytraced.flushRaytraced();
                    raytraced.collect = false;
                    original.call(instance, stack, displayContext, leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, model);
                    raytraced.flushRaytraced();
                    raytraced.collect = true;
                    return;
                }
            }
        }

        original.call(instance, stack, displayContext, leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, model);
    }

    @Inject(
            method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V",
            at = @At(
                    value = "INVOKE",
                    //? if >=1.21 {
                    target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"
                    //? } else {
                    /*target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V"
                    *///? }
            )
    )
    private void innerBlit(
            ResourceLocation atlasLocation,
            int x1,
            int x2,
            int y1,
            int y2,
            int blitOffset,
            float minU,
            float maxU,
            float minV,
            float maxV,
            CallbackInfo ci,
            @Local Matrix4f matrix4f
    ) {
        if ((Object) this instanceof RaytracedGuiGraphics raytraced) {
            var builder = raytraced.blitBuilders.computeIfAbsent(GlTexture2D.get(new NeoIdentifier(atlasLocation.getNamespace(), atlasLocation.getPath())), raytraced::createConsumer);
            builder.vertex(matrix4f, x1, y1, blitOffset).textureUV(minU, minV);
            builder.vertex(matrix4f, x1, y2, blitOffset).textureUV(minU, maxV);
            builder.vertex(matrix4f, x2, y2, blitOffset).textureUV(maxU, maxV);
            builder.vertex(matrix4f, x2, y1, blitOffset).textureUV(maxU, minV);
            builder.flush();
        }
    }

    @Inject(
            method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    //? if >=1.21 {
                    target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"
                    //? } else {
                    /*target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V"
                    *///? }
            )
    )
    private void innerBlit(
            ResourceLocation atlasLocation,
            int x1,
            int x2,
            int y1,
            int y2,
            int blitOffset,
            float minU,
            float maxU,
            float minV,
            float maxV,
            float red,
            float green,
            float blue,
            float alpha,
            CallbackInfo ci,
            @Local Matrix4f matrix4f
    ) {
        if ((Object) this instanceof RaytracedGuiGraphics raytraced) {
            var builder = raytraced.blitBuilders.computeIfAbsent(GlTexture2D.get(new NeoIdentifier(atlasLocation.getNamespace(), atlasLocation.getPath())), raytraced::createConsumer);
            builder.vertex(matrix4f, x1, y1, blitOffset).textureUV(minU, minV).color(red, green, blue, alpha);
            builder.vertex(matrix4f, x1, y2, blitOffset).textureUV(minU, maxV).color(red, green, blue, alpha);
            builder.vertex(matrix4f, x2, y2, blitOffset).textureUV(maxU, maxV).color(red, green, blue, alpha);
            builder.vertex(matrix4f, x2, y1, blitOffset).textureUV(maxU, minV).color(red, green, blue, alpha);
            builder.flush();
        }
    }
}
