package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.IBuffer
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.api.impl.NeoIndexedBuffer
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.resource.BufferUsage
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.light.Light
import net.typho.vibrancy.light.LightManager
import net.typho.vibrancy.shadows.LightFace.Companion.toLightFace
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.function.Consumer

abstract class ShadowManager<L : Light>(
    static: Boolean
) : NativeResource, MultiBufferSource {
    val shadowMesh by lazy { VertexBuffer(if (static) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC) }
    val quadBuffer by lazy {
        NeoIndexedBuffer(
            null,
            GlResourceType.SHADER_STORAGE_BUFFER,
            if (static) BufferUsage.STATIC_DRAW else BufferUsage.STREAM_DRAW
        )
    }
    protected var shadows: MutableList<ShadowVolume> = LinkedList()
    protected var shadowsDirty = false
    protected val shadowBuilders = LinkedHashMap<RenderType, ShadowBuilder>()
    protected var numBlockEntities: Int = 0
    protected var numEntities: Int = 0

    companion object {
        val DYNAMIC_SHADOW_MESH by lazy { VertexBuffer(VertexBuffer.Usage.DYNAMIC) }
        val DYNAMIC_QUAD_BUFFER by lazy {
            NeoIndexedBuffer(
                Vibrancy.id("dynamic_shadow_quads"),
                GlResourceType.SHADER_STORAGE_BUFFER,
                BufferUsage.STREAM_DRAW
            )
        }
    }

    override fun free() {
        shadowMesh.close()
        quadBuffer.release()
    }

    open fun numQuads() = shadows.stream()
        .mapToInt { it.numQuads() }
        .sum()

    open fun numShadows() = shadows.size

    open fun numBlockEntities() = numBlockEntities

    open fun numEntities() = numEntities

    open fun isTaskActive() = false

    override fun getBuffer(renderType: RenderType): VertexConsumer =
        shadowBuilders.computeIfAbsent(renderType) { ShadowBuilder() }

    abstract fun shouldCastFace(
        face: Direction,
        light: L,
        pos: BlockPos,
        level: BlockGetter,
        state: BlockState
    ): Boolean

    open fun rebuildBlock(manager: LightManager, pos: BlockPos, light: L) {
        shadows.removeIf { shadow -> shadow.caster.blockPos?.equals(pos) ?: false }

        getLightFaces(
            manager.getLevel(),
            light,
            pos
        ) { face ->
            shadows.add(lightFaceToVolume(face, manager, light))
        }
        shadowsDirty = true
    }

    abstract fun lightFaceToVolume(face: LightFace, manager: LightManager, light: L): ShadowVolume

    @Suppress("DEPRECATION")
    open fun getLightFaces(
        level: ClientLevel,
        light: L,
        pos: BlockPos,
        out: Consumer<LightFace>
    ) {
        val state = level.getBlockState(pos)
        val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
        val random = RandomSource.create()
        val offset = state.getOffset(level, pos)

        for (dir in Direction.entries) {
            if (shouldCastFace(dir, light, pos, level, state)) {
                for (quad in model.getQuads(state, dir, random)) {
                    out.accept(
                        quad.toLightFace(
                            offset.x.toFloat(),
                            offset.y.toFloat(),
                            offset.z.toFloat(),
                            pos,
                            dir
                        )
                    )
                }
            }
        }

        for (quad in model.getQuads(state, null, random)) {
            out.accept(
                quad.toLightFace(
                    offset.x.toFloat(),
                    offset.y.toFloat(),
                    offset.z.toFloat(),
                    pos,
                    null
                )
            )
        }
    }

    protected open fun uploadShadows(
        light: L,
        shadowMesh: VertexBuffer,
        quadBuffer: IBuffer,
        shadows: Collection<ShadowVolume>
    ) {
        if (shadows.isNotEmpty()) {
            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
            val quads = MemoryUtil.memAlloc(shadows.size * LightFace.BYTES)

            for (shadow in shadows) {
                shadow.buildGeometry(builder)
                shadow.toLightFace().put(quads)
            }

            val built = builder.build()!!

            shadowMesh.bind()
            shadowMesh.upload(built)

            quadBuffer.bind().use {
                quadBuffer.upload(MemoryUtil.memAddress(quads.flip()))
            }

            MemoryUtil.memFree(quads)
        }
    }

    protected open fun castEntities(
        manager: LightManager,
        blockEntityBox: BlockBox?,
        entityBox: BlockBox?,
        light: L
    ): Boolean {
        val level = manager.getLevel()
        val poseStack = PoseStack()
        var any = false
        val tickDelta = manager.tickDelta()

        blockEntityBox?.let {
            for (pos in it) {
                val blockEntity = level.getBlockEntity(pos)

                if (blockEntity != null && level.getBlockState(pos).renderShape == RenderShape.ENTITYBLOCK_ANIMATED) {
                    poseStack.pushPose()
                    poseStack.translate(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

                    Minecraft.getInstance().blockEntityRenderDispatcher.render(
                        blockEntity,
                        tickDelta,
                        poseStack,
                        this
                    )

                    poseStack.popPose()

                    any = true
                    numBlockEntities++
                }
            }
        }

        entityBox?.let {
            for (entity in level.getEntities(null, it.aabb())) {
                val pos = entity.getPosition(tickDelta)
                Minecraft.getInstance().entityRenderDispatcher.render(
                    entity,
                    pos.x,
                    pos.y,
                    pos.z,
                    Mth.lerp(tickDelta, entity.yRotO, entity.yRot),
                    tickDelta,
                    poseStack,
                    this,
                    LightTexture.FULL_BRIGHT
                )
                any = true
                numEntities++
            }
        }

        return any
    }

    abstract fun initializeUniforms(manager: LightManager, light: L, shader: IShader)

    abstract fun getEntityBox(manager: LightManager, light: L): BlockBox?

    abstract fun getBlockEntityBox(manager: LightManager, light: L): BlockBox?

    open fun render(manager: LightManager, raytrace: Boolean, light: L, shader: IShader, stack: GlStack) {
        numBlockEntities = 0
        numEntities = 0

        if (raytrace) {
            //glPatchParameteri(GL_PATCH_VERTICES, 4);

            initializeUniforms(manager, light, shader)

            if (shadowsDirty) {
                uploadShadows(light, shadowMesh, quadBuffer, shadows)
                shadowsDirty = false
            }

            if (shadows.isNotEmpty()) {
                shader.setSampler(
                    "AtlasSampler",
                    Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS)
                )

                quadBuffer.bindBase(stack, 0)

                shadowMesh.bind()
                shadowMesh.draw()
            }

            for (builder in shadowBuilders.values) {
                builder.vertices.clear()
            }

            if (Vibrancy.ENTITY_SHADOWS) {
                val anyEntities =
                    castEntities(manager, getBlockEntityBox(manager, light), getEntityBox(manager, light), light)

                if (anyEntities) {
                    val entityShadows = HashMap<ResourceLocation, MutableList<ShadowVolume>>()

                    for (entry in shadowBuilders) {
                        entry.value.endVertex()

                        if (entry.key.mode() == VertexFormat.Mode.QUADS && !entry.value.vertices.isEmpty()) {
                            val texture = Vibrancy.getRenderTypeTexture(entry.key)
                            val output = entityShadows.computeIfAbsent(texture) { LinkedList() }
                            val iterator = entry.value.vertices.iterator()

                            while (iterator.hasNext()) {
                                val v1 = iterator.next()
                                val v2 = iterator.next()
                                val v3 = iterator.next()
                                val v4 = iterator.next()

                                output.add(
                                    lightFaceToVolume(
                                        LightFace(
                                            null,
                                            null,
                                            v1.vertex,
                                            v2.vertex,
                                            v3.vertex,
                                            v4.vertex,
                                            v1.uv,
                                            v2.uv,
                                            v3.uv,
                                            v4.uv
                                        ),
                                        manager,
                                        light
                                    )
                                )
                            }
                        }
                    }

                    for (entry in entityShadows) {
                        uploadShadows(light, DYNAMIC_SHADOW_MESH, DYNAMIC_QUAD_BUFFER, entry.value)

                        shader.setSampler(
                            "AtlasSampler",
                            Minecraft.getInstance().textureManager.getTexture(entry.key)
                        )

                        DYNAMIC_QUAD_BUFFER.bindBase(stack, 0)

                        DYNAMIC_SHADOW_MESH.bind()
                        DYNAMIC_SHADOW_MESH.draw()
                    }
                }
            }
        }
    }
}