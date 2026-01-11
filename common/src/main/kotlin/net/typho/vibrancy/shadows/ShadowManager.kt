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
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.light.Light
import net.typho.vibrancy.light.LightManager
import net.typho.vibrancy.light.TextureCoordinates
import net.typho.vibrancy.shadows.LightFace.Companion.toLightFace
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.function.Consumer

abstract class ShadowManager<L : Light>(
    static: Boolean
) : NativeResource, MultiBufferSource {
    val shadowMesh by lazy { VertexBuffer(if (static) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC) }
    protected var numShadows = 0
    protected var shadows: MutableList<LightFace> = LinkedList()
    protected var uploadShadows = false
    protected val shadowBuilders = LinkedHashMap<RenderType, ShadowBuilder>()
    protected var numBlockEntities: Int = 0
    protected var numEntities: Int = 0

    companion object {
        val DYNAMIC_SHADOW_MESH by lazy { VertexBuffer(VertexBuffer.Usage.DYNAMIC) }
    }

    override fun free() {
        shadowMesh.close()
    }

    open fun numShadows() = numShadows

    open fun numBlockEntities() = numBlockEntities

    open fun numEntities() = numEntities

    open fun isTaskActive() = false

    override fun getBuffer(renderType: RenderType): VertexConsumer =
        shadowBuilders.computeIfAbsent(renderType) { ShadowBuilder() }

    abstract fun shouldCastFace(
        face: Direction?,
        light: L,
        pos: BlockPos,
        level: BlockGetter,
        state: BlockState
    ): Boolean

    open fun rebuildBlock(manager: LightManager, pos: BlockPos, light: L) {
        shadows.removeIf { shadow -> shadow.blockPos == pos }

        uploadShadows = uploadShadows or getLightFaces(
            manager.getLevel(),
            light,
            pos,
            shadows::add
        )

        numShadows = shadows.size
    }

    @Suppress("DEPRECATION")
    open fun getLightFaces(
        level: ClientLevel,
        light: L,
        pos: BlockPos,
        out: Consumer<LightFace>
    ): Boolean {
        val state = level.getBlockState(pos)

        if (!light.shouldCastBlock(state, level, pos)) {
            return false
        }

        var any = false
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
                            pos
                        )
                    )
                    any = true
                }
            }
        }

        for (quad in model.getQuads(state, null, random)) {
            out.accept(
                quad.toLightFace(
                    offset.x.toFloat(),
                    offset.y.toFloat(),
                    offset.z.toFloat(),
                    pos
                )
            )
            any = true
        }

        return any
    }

    protected open fun uploadShadows(
        light: L,
        shadowMesh: VertexBuffer,
        shadows: Collection<LightFace>
    ) {
        if (shadows.isNotEmpty()) {
            numShadows = shadows.size

            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)

            for (shadow in shadows) {
                shadow.buildGeometry(builder)
            }

            val built = builder.build()!!

            shadowMesh.bind()
            shadowMesh.upload(built)
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
            initializeUniforms(manager, light, shader)

            if (uploadShadows) {
                uploadShadows(light, shadowMesh, shadows)
                uploadShadows = false
            }

            if (shadows.isNotEmpty()) {
                shader.setSampler(
                    "AtlasSampler",
                    Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS)
                )

                shadowMesh.bind()
                shadowMesh.draw()
            }

            for (builder in shadowBuilders.values) {
                builder.vertices.clear()
            }

            if (Vibrancy.config.visuals.entityShadows) {
                val anyEntities = castEntities(manager, getBlockEntityBox(manager, light), getEntityBox(manager, light), light)

                if (anyEntities) {
                    val entityShadows = HashMap<ResourceLocation, MutableList<LightFace>>()

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
                                    LightFace(
                                        null,
                                        v1.vertex,
                                        v2.vertex,
                                        v3.vertex,
                                        v4.vertex,
                                        TextureCoordinates(
                                            v1.uv,
                                            v2.uv,
                                            v3.uv,
                                            v4.uv,
                                        ),
                                        1,
                                        1
                                    )
                                )
                            }
                        }
                    }

                    for (entry in entityShadows) {
                        uploadShadows(light, DYNAMIC_SHADOW_MESH, entry.value)

                        shader.setSampler(
                            "AtlasSampler",
                            Minecraft.getInstance().textureManager.getTexture(entry.key)
                        )

                        DYNAMIC_SHADOW_MESH.bind()
                        DYNAMIC_SHADOW_MESH.draw()
                    }
                }
            }
        }
    }
}