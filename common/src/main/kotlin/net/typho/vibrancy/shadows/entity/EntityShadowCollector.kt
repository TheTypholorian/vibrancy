package net.typho.vibrancy.shadows.entity

import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.api.builtin.EmptyVertexConsumer
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.shadows.ShadowVertexBuffer
import kotlin.jvm.optionals.getOrNull

class EntityShadowCollector {
    val buffers = HashMap<ResourceLocation, ShadowVertexBuffer>()
    var numEntities = 0
    var numBlockEntities = 0

    fun collect(manager: LightManager, lights: Collection<*>) {
        buffers.values.forEach { buffer -> buffer.size = 0 }

        val level = manager.getLevel()
        val entities = HashSet<Entity>()
        val blockEntities = HashSet<BlockEntity>()
        val cameraPos = manager.getCamera().position
        val frustum = manager.getCullingFrustum()

        for (light in lights) {
            if (light is EntityShadowCastingLight) {
                light.getEntityShadowBox()?.let { box ->
                    entities.addAll(level.getEntities(null, box) { e ->
                        !e.isInvisible && !e.isSpectator && Minecraft.getInstance().entityRenderDispatcher.shouldRender(
                            e,
                            frustum,
                            cameraPos.x,
                            cameraPos.y,
                            cameraPos.z
                        )
                    })

                    for (pos in BlockBox(
                        BlockPos.containing(box.minPosition),
                        BlockPos.containing(box.maxPosition),
                    )) {
                        val blockEntity = level.getBlockEntity(pos)

                        if (blockEntity != null && level.getBlockState(pos).renderShape == RenderShape.ENTITYBLOCK_ANIMATED) {
                            blockEntities.add(blockEntity)
                        }
                    }
                }
            }
        }

        val poseStack = PoseStack()
        val tickDelta = manager.getTickDelta(true)
        val builders = HashMap<ResourceLocation, BufferBuilder>()
        val multiBufferSource = MultiBufferSource { renderType ->
            val texture = getRenderTypeTexture(renderType)

            return@MultiBufferSource if (
                texture == null
                || renderType.mode().primitiveLength < 3
                || !renderType.format().contains(VertexFormatElement.POSITION)
                || !renderType.format().contains(VertexFormatElement.UV0)
            ) {
                EmptyVertexConsumer
            } else {
                builders.computeIfAbsent(texture) {
                    Tesselator.getInstance().begin(renderType.mode(), DefaultVertexFormat.POSITION_TEX)
                }
            }
        }

        for (entity in entities) {
            val pos = entity.getPosition(tickDelta)
            Minecraft.getInstance().entityRenderDispatcher.render(
                entity,
                pos.x,
                pos.y,
                pos.z,
                Mth.lerp(tickDelta, entity.yRotO, entity.yRot),
                tickDelta,
                poseStack,
                multiBufferSource,
                LightTexture.FULL_BRIGHT
            )
        }

        for (entity in blockEntities) {
            poseStack.pushPose()
            poseStack.translate(entity.blockPos.x.toDouble(), entity.blockPos.y.toDouble(), entity.blockPos.z.toDouble())

            Minecraft.getInstance().blockEntityRenderDispatcher.render(
                entity,
                tickDelta,
                poseStack,
                multiBufferSource
            )

            poseStack.popPose()
        }

        for (entry in builders) {
            buffers.computeIfAbsent(entry.key) { key ->
                ShadowVertexBuffer(
                    VertexBuffer.Usage.DYNAMIC,
                    Minecraft.getInstance().textureManager.getTexture(key).id
                )
            }.upload(entry.value)
        }

        numEntities = entities.size
        numBlockEntities = blockEntities.size
    }

    fun render(shader: IShader) {
        for (buffer in buffers.values) {
            buffer.render(shader)
        }
    }

    fun getRenderTypeTexture(renderType: RenderType): ResourceLocation? {
        return when (renderType) {
            is RenderType.CompositeRenderType -> {
                renderType.state().textureState.cutoutTexture().getOrNull()
            }
            else -> null
        }
    }
}