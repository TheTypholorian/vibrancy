package net.typho.vibrancy.util

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.OutlineBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.typho.big_shot_lib.api.client.rendering.util.NeoMultiBufferSource
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.vibrancy.Vibrancy

//? if <1.21.9 {
/*import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
*///? } else {
//? if <1.21.11 {
import net.minecraft.client.renderer.RenderType
//? } else {
/*import net.minecraft.client.renderer.rendertype.RenderType
*///? }
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.world.level.levelgen.SurfaceRules.state
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML

//? }

object EntityRenderingUtil {
    fun <E : Entity> render(entity: E, pose: PoseStack, buffers: NeoMultiBufferSource, tickDelta: Float = Vibrancy.tickDelta, light: Int = LightTexture.FULL_BRIGHT) {
        //? if <1.21.5 {
        /*Minecraft.getInstance().entityRenderDispatcher.render(
            entity,
            Mth.lerp(tickDelta.toDouble(), entity.xOld, entity.x),
            Mth.lerp(tickDelta.toDouble(), entity.yOld, entity.y),
            Mth.lerp(tickDelta.toDouble(), entity.zOld, entity.z),
            Mth.lerp(tickDelta, entity.yRotO, entity.yRot),
            tickDelta,
            pose,
            WrapperUtil.INSTANCE.unwrap(buffers),
            light
        )
        *///? } else if <1.21.9 {
        /*Minecraft.getInstance().entityRenderDispatcher.render(
            entity,
            Mth.lerp(tickDelta.toDouble(), entity.xOld, entity.x),
            Mth.lerp(tickDelta.toDouble(), entity.yOld, entity.y),
            Mth.lerp(tickDelta.toDouble(), entity.zOld, entity.z),
            tickDelta,
            pose,
            WrapperUtil.INSTANCE.unwrap(buffers),
            light
        )
        *///? } else {
        @Suppress("UNCHECKED_CAST")
        fun <S : EntityRenderState> render(renderer: EntityRenderer<*, S>, state: EntityRenderState) {
            val state = state as S
            val offset = renderer.getRenderOffset(state)

            pose.pushPose()
            pose.translate(state.x + offset.x, state.y + offset.y, state.z + offset.z)

            val storage = SubmitNodeStorage()
            val features = FeatureRenderDispatcher(
                storage,
                Minecraft.getInstance().blockRenderer,
                WrapperUtil.INSTANCE.unwrapStupid(
                    { buffers.getBuffer(it) },
                    { }
                ),
                Minecraft.getInstance().atlasManager,
                object : OutlineBufferSource() {
                    override fun getBuffer(renderType: RenderType): VertexConsumer {
                        return WrapperUtil.INSTANCE.unwrap(EmptyVertexConsumer)
                    }
                },
                WrapperUtil.INSTANCE.unwrapStupid(
                    { EmptyVertexConsumer },
                    { }
                ),
                Minecraft.getInstance().font
            )

            renderer.submit(state, pose, storage, Minecraft.getInstance().gameRenderer.levelRenderState.cameraRenderState)

            storage.endFrame()
            features.renderAllFeatures()
            features.endFrame()

            pose.popPose()
        }

        val renderer = Minecraft.getInstance().entityRenderDispatcher.getRenderer(entity)
        render(renderer, renderer.createRenderState(entity, tickDelta))
        //? }
    }

    fun <E : BlockEntity> renderBlockEntity(blockEntity: E, pose: PoseStack, buffers: NeoMultiBufferSource, data: RenderEventData, tickDelta: Float = Vibrancy.tickDelta, light: Int = LightTexture.FULL_BRIGHT) {
        //? if <1.21.5 {
        /*Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(blockEntity)!!.render(
            blockEntity,
            tickDelta,
            pose,
            WrapperUtil.INSTANCE.unwrap(buffers),
            light,
            OverlayTexture.NO_OVERLAY
        )
        *///? } else if <1.21.9 {
        /*Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(blockEntity)!!.render(
            blockEntity,
            tickDelta,
            pose,
            WrapperUtil.INSTANCE.unwrap(buffers),
            light,
            OverlayTexture.NO_OVERLAY,
            Vec3(data.camera.pos.toJOML())
        )
        *///? } else {
        fun <S : BlockEntityRenderState> render(state: S) {
            val renderer = Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer<E, S>(blockEntity)!!

            val storage = SubmitNodeStorage()
            val features = FeatureRenderDispatcher(
                storage,
                Minecraft.getInstance().blockRenderer,
                WrapperUtil.INSTANCE.unwrapStupid(
                    { buffers.getBuffer(it) },
                    { }
                ),
                Minecraft.getInstance().atlasManager,
                object : OutlineBufferSource() {
                    override fun getBuffer(renderType: RenderType): VertexConsumer {
                        return WrapperUtil.INSTANCE.unwrap(EmptyVertexConsumer)
                    }
                },
                WrapperUtil.INSTANCE.unwrapStupid(
                    { EmptyVertexConsumer },
                    { }
                ),
                Minecraft.getInstance().font
            )

            renderer.submit(state, pose, storage, Minecraft.getInstance().gameRenderer.levelRenderState.cameraRenderState)

            storage.endFrame()
            features.renderAllFeatures()
            features.endFrame()
        }

        //? fabric {
        /*render(Minecraft.getInstance().blockEntityRenderDispatcher.tryExtractRenderState(blockEntity, tickDelta, null)!!)
        *///? } neoforge {
        render(Minecraft.getInstance().blockEntityRenderDispatcher.tryExtractRenderState(blockEntity, tickDelta, null, null)!!)
        //? }
        //? }
    }
}