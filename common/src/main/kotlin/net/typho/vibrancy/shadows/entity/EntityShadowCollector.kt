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
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.api.builtin.EmptyVertexConsumer
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightStorage
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.shadows.ShadowVertexBuffer
import kotlin.jvm.optionals.getOrNull

open class EntityShadowCollector {
    val buffers = HashMap<ResourceLocation, ShadowVertexBuffer>()
    val boxes = HashSet<AABB>()
    var numEntities = 0
    var numBlockEntities = 0

    @Suppress("UNCHECKED_CAST")
    protected fun <S : BlockLightStorage<*>> getShadowBoxes(manager: LightManager, type: BlockLightType<*, *, S>, storage: BlockLightStorage<*>): Iterable<AABB>? {
        return type.getEntityShadowBoxes(manager, storage as S)
    }

    fun collect(manager: LightManager, lights: Map<BlockLightType<*, *, *>, BlockLightStorage<*>>) {
        buffers.values.forEach { buffer -> buffer.size = 0 }
        boxes.clear()

        val level = manager.getLevel()
        val entities = HashSet<Entity>()
        val blockEntities = HashSet<BlockEntity>()
        val camera = manager.getCamera()
        val cameraPos = camera.position
        val frustum = manager.getCullingFrustum()
        val distanceSq = Vibrancy.config.entityShadows.distance.get() * Vibrancy.config.entityShadows.distance.get() * 16 * 16

        for (light in lights) {
            getShadowBoxes(manager, light.key, light.value)?.forEach { box ->
                entities.addAll(level.getEntities(null, box) { e ->
                    !e.isInvisible && !e.isSpectator && Minecraft.getInstance().entityRenderDispatcher.shouldRender(
                        e,
                        frustum,
                        cameraPos.x,
                        cameraPos.y,
                        cameraPos.z
                    ) && e.distanceToSqr(cameraPos) < distanceSq
                })

                for (pos in BlockBox(
                    BlockPos.containing(box.minPosition),
                    BlockPos.containing(box.maxPosition),
                )) {
                    val blockEntity = level.getBlockEntity(pos)

                    if (blockEntity != null && level.getBlockState(pos).renderShape == RenderShape.ENTITYBLOCK_ANIMATED && pos.center.distanceToSqr(cameraPos) < distanceSq) {
                        blockEntities.add(blockEntity)
                    }
                }
            }
        }

        if (!Vibrancy.config.entityShadows.firstPersonShadow) {
            if (!(camera.isDetached || camera.entity is LivingEntity && (camera.entity as LivingEntity).isSleeping)) {
                entities.remove(camera.entity)
            }
        }

        val poseStack = PoseStack()
        val tickDelta = manager.getTickDelta(false)
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
                    BufferBuilder(ByteBufferBuilder(renderType.bufferSize()), renderType.mode(), DefaultVertexFormat.POSITION_TEX)
                }
            }
        }

        val entityDispatcher = Minecraft.getInstance().entityRenderDispatcher

        val hitboxes = entityDispatcher.shouldRenderHitBoxes()

        entityDispatcher.setRenderShadow(false)
        entityDispatcher.setRenderHitBoxes(false)

        for (entity in entities) {
            val pos = entity.getPosition(tickDelta)

            entityDispatcher.render(
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

        entityDispatcher.setRenderShadow(true)
        entityDispatcher.setRenderHitBoxes(hitboxes)

        for (entity in blockEntities) {
            poseStack.pushPose()
            poseStack.translate(
                entity.blockPos.x.toDouble(),
                entity.blockPos.y.toDouble(),
                entity.blockPos.z.toDouble()
            )

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
        boxes.addAll(entities.map { entity -> entity.boundingBoxForCulling })
        boxes.addAll(blockEntities.map { blockEntity -> AABB(blockEntity.blockPos) })
    }

    fun shouldRender(manager: LightManager, box: AABB): Boolean {
        val distanceSq = Vibrancy.config.entityShadows.distance.get() * Vibrancy.config.entityShadows.distance.get() * 16 * 16
        return box.distanceToSqr(manager.getCamera().position) < distanceSq// && boxes.any { box1 -> box.intersects(box1) }
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