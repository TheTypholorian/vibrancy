package net.typho.vibrancy.util

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.vibrancy.Vibrancy

object EntityRenderingUtil {
    fun <E : Entity> render(entity: E, pose: PoseStack, buffers: MultiBufferSource, tickDelta: Float = Vibrancy.tickDelta, light: Int = LightTexture.FULL_BRIGHT) {
        //? if <1.21.5 {
        /*Minecraft.getInstance().entityRenderDispatcher.render(
            entity,
            Mth.lerp(tickDelta.toDouble(), entity.xOld, entity.x),
            Mth.lerp(tickDelta.toDouble(), entity.yOld, entity.y),
            Mth.lerp(tickDelta.toDouble(), entity.zOld, entity.z),
            Mth.lerp(tickDelta, entity.yRotO, entity.yRot),
            tickDelta,
            pose,
            buffers,
            light
        )
        *///? } else if <1.21.9 {
        Minecraft.getInstance().entityRenderDispatcher.render(
            entity,
            Mth.lerp(tickDelta.toDouble(), entity.xOld, entity.x),
            Mth.lerp(tickDelta.toDouble(), entity.yOld, entity.y),
            Mth.lerp(tickDelta.toDouble(), entity.zOld, entity.z),
            tickDelta,
            pose,
            buffers,
            light
        )
        //? } else {
        /*val renderer = Minecraft.getInstance().entityRenderDispatcher.getRenderer(entity)

        if (renderer.shouldRender(entity, Frustum(data.modelViewMat, data.projMat), data.camera.pos.x.toDouble(), data.camera.pos.y.toDouble(), data.camera.pos.z.toDouble())) {
            val storage = SubmitNodeStorage()
            val features = FeatureRenderDispatcher(
                storage,
                Minecraft.getInstance().blockRenderer,
                node.bufferSource,
                Minecraft.getInstance().atlasManager,
                object : OutlineBufferSource() {
                    override fun getBuffer(renderType: RenderType): VertexConsumer {
                        return WrapperUtil.INSTANCE.unwrap(EmptyVertexConsumer)
                    }
                },
                WrapperUtil.INSTANCE.unwrap { EmptyVertexConsumer },
                Minecraft.getInstance().font
            )

            renderer.submit(
                renderer.createRenderState(entity, Vibrancy.tickDelta),
                poseStack,
                storage,
                Minecraft.getInstance().gameRenderer.levelRenderState.cameraRenderState
            )

            storage.endFrame()
        }
        *///? }
    }

    fun <E : BlockEntity> renderBlockEntity(blockEntity: E, pose: PoseStack, buffers: MultiBufferSource, data: RenderEventData, tickDelta: Float = Vibrancy.tickDelta, light: Int = LightTexture.FULL_BRIGHT) {
        //? if <1.21.5 {
        /*Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(blockEntity)!!.render(
            blockEntity,
            tickDelta,
            pose,
            buffers,
            light,
            OverlayTexture.NO_OVERLAY
        )
        *///? } else if <1.21.9 {
        Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(blockEntity)!!.render(
            blockEntity,
            tickDelta,
            pose,
            buffers,
            light,
            OverlayTexture.NO_OVERLAY,
            Vec3(data.camera.pos.toJOML())
        )
        //? } else {
        /*TODO()
        *///? }
    }
}