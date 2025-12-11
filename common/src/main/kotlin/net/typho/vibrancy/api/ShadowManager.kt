package net.typho.vibrancy.api

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import foundry.veil.api.client.render.rendertype.VeilRenderType
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.api.LightFace.Companion.toLightFace
import net.typho.vibrancy.platform.Services
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

open class ShadowManager(
    static: Boolean
) : NativeResource, MultiBufferSource {
    private val debugMesh: VertexBuffer? = if (Services.PLATFORM.isDevelopmentEnvironment()) VertexBuffer(VertexBuffer.Usage.STATIC) else null
    var shadowMesh: VertexBuffer? = VertexBuffer(if (static) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC)
    var quadBuffer: ShaderStorageBuffer? = ShaderStorageBuffer(if (static) ShaderStorageBuffer.Usage.STATIC else ShaderStorageBuffer.Usage.STREAM)
    private var shadows: MutableList<ShadowVolume> = LinkedList()
    private var fullRebuildTask: CompletableFuture<MutableList<ShadowVolume>>? = null
    private var shadowsDirty = false
    private val buffers = LinkedHashMap<RenderType, ShadowBuilder>()

    override fun free() {
        shadowMesh?.close()
        shadowMesh = null
        quadBuffer?.close()
        quadBuffer = null
    }

    fun numQuads() = shadows.stream()
        .mapToInt { it.numQuads() }
        .sum()

    fun numShadows() = shadows.size

    fun isTaskActive() = !(fullRebuildTask?.isDone ?: false)

    override fun getBuffer(renderType: RenderType): VertexConsumer = buffers.computeIfAbsent(renderType) { ShadowBuilder() }

    fun shouldCastFace(
        face: Direction,
        lightPos: BlockPos,
        pos: BlockPos,
        level: BlockGetter,
        state: BlockState
    ): Boolean {
        val otherPos = pos.relative(face)

        if (otherPos.equals(lightPos)) {
            return true
        }

        if (!Vibrancy.pointsToward(face, lightPos.subtract(pos).center.toVector3f())) {
            return false
        }

        val otherState = level.getBlockState(otherPos)

        return !(state.isSolidRender(level, pos) && otherState.isSolidRender(level, otherPos))
    }

    @Suppress("DEPRECATION")
    fun getLightFaces(
        level: ClientLevel,
        lightBlockPos: BlockPos,
        pos: BlockPos,
        out: Consumer<LightFace>
    ) {
        val state = level.getBlockState(pos)
        val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
        val random = RandomSource.create()
        val offset = state.getOffset(level, pos)

        for (dir in Direction.entries) {
            if (shouldCastFace(dir, lightBlockPos, pos, level, state)) {
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

    fun rebuildBlock(manager: LightManager, pos: BlockPos, light: PointLight) {
        shadows.removeIf { shadow -> shadow.caster.blockPos == pos }

        val lightPos = light.getPosition()
        val lightBlockPos = light.getBlockPos()

        getLightFaces(
            manager.getLevel(),
            lightBlockPos,
            pos
        ) { face ->
            shadows.add(face.toVolumePoint(lightPos, light.getRadius()))
        }
        shadowsDirty = true
    }

    fun fullRebuild(manager: LightManager, box: BlockBox, light: PointLight) {
        fullRebuildTask?.cancel(true)
        fullRebuildTask = CompletableFuture.supplyAsync {
            val lightPos = light.getPosition()
            val lightBlockPos = light.getBlockPos()
            val radius = light.getShadowRadius(manager)
            val radiusSq = radius * radius
            val volumes = LinkedList<ShadowVolume>()

            for (x in box.min.x..box.max.x) {
                for (y in box.min.y..box.max.y) {
                    for (z in box.min.z..box.max.z) {
                        val pos = BlockPos(x, y, z)

                        if (pos != lightBlockPos && pos.distSqr(lightBlockPos) <= radiusSq) {
                            getLightFaces(
                                manager.getLevel(),
                                lightBlockPos,
                                pos
                            ) { face ->
                                volumes.add(face.toVolumePoint(lightPos, light.getRadius()))
                            }
                        }
                    }
                }
            }

            shadowsDirty = true

            return@supplyAsync volumes
        }
    }

    private fun uploadShadows(light: PointLight) {
        if (shadows.isNotEmpty()) {
            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
            val quads = MemoryUtil.memAlloc(shadows.size * LightFace.BYTES)

            for (shadow in shadows) {
                shadow.buildGeometry(builder)
                shadow.toLightFace().put(quads)
            }

            val built = builder.build()!!

            shadowMesh!!.bind()
            shadowMesh!!.upload(built)
            VertexBuffer.unbind()

            quadBuffer!!.bind()
            quadBuffer!!.upload(quads.flip())
            ShaderStorageBuffer.unbind()

            MemoryUtil.memFree(quads)

            debugMesh?.let {
                val lightBlockPos = light.getBlockPos()
                val debugBuilder =
                    Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR)

                for (shadow in shadows) {
                    shadow.buildDebug(lightBlockPos, debugBuilder)
                }

                val debugBuilt = debugBuilder.build()!!

                it.bind()
                it.upload(debugBuilt)
                VertexBuffer.unbind()
            }
        }
    }

    fun castBlockEntities(manager: LightManager, box: BlockBox, light: PointLight): Boolean {
        val level = manager.getLevel()
        val poseStack = PoseStack()
        var any = false

        for (pos in box) {
            val blockEntity = level.getBlockEntity(pos)

            if (blockEntity != null) {
                poseStack.pushPose()
                poseStack.translate(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

                Minecraft.getInstance().blockEntityRenderDispatcher.render(
                    blockEntity,
                    manager.tickDelta(),
                    poseStack,
                    this
                )

                poseStack.popPose()

                any = true
            }
        }

        return any
    }

    fun render(manager: LightManager, raytrace: Boolean, light: PointLight) {
        if (fullRebuildTask?.isDone ?: false) {
            shadows = fullRebuildTask!!.get()
            fullRebuildTask = null
        }

        if (raytrace) {
            if (shadowsDirty) {
                uploadShadows(light)
                shadowsDirty = false
            }

            if (shadows.isNotEmpty()) {
                val shader = RenderSystem.getShader()!!

                shader.safeGetUniform("LightPos").set(light.getPosition())
                shader.setSampler(
                    "AtlasSampler",
                    Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS)
                )

                quadBuffer!!.bindBase(0)

                shadowMesh!!.bind()
                shadowMesh!!.drawWithShader(
                    manager.viewMatrix!!,
                    RenderSystem.getProjectionMatrix(),
                    shader
                )
            }

            for (builder in buffers.values) {
                builder.vertices.clear()
            }

            val anyEntities = castBlockEntities(
                manager,
                BlockBox.of(light.getBlockPos()).expand(light.getShadowRadius(manager)),
                light
            )

            if (anyEntities) {
                for (entry in buffers) {
                    val texture = Services.PLATFORM.getRenderTypeTexture(entry.key)
                }
            }

            if ((shadows.isNotEmpty() || anyEntities) && Vibrancy.RENDER_DEBUG_LINES) {
                debugMesh?.let {
                    val lineRenderType = VeilRenderType.get(Vibrancy.id("debug"))!!
                    lineRenderType.setupRenderState()

                    it.bind()
                    it.drawWithShader(
                        manager.viewMatrix!!,
                        RenderSystem.getProjectionMatrix(),
                        RenderSystem.getShader()!!
                    )
                    VertexBuffer.unbind()

                    lineRenderType.clearRenderState()
                }
            }
        }
    }
}