package net.typho.vibrancy.api

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import foundry.veil.api.client.render.rendertype.VeilRenderType
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.api.LightFace.Companion.toLightFace
import net.typho.vibrancy.platform.Services
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class ShadowManager(
    val static: Boolean
) : NativeResource {
    private val debugMesh: VertexBuffer? = if (Services.PLATFORM.isDevelopmentEnvironment()) VertexBuffer(VertexBuffer.Usage.STATIC) else null
    var shadowMesh: VertexBuffer? = null
    var quadBuffer: ShaderStorageBuffer? = null
    private var shadows: List<ShadowVolume> = LinkedList()
    private var fullRebuildTask: CompletableFuture<List<ShadowVolume>>? = null

    init {
        RenderSystem.recordRenderCall {
            shadowMesh = VertexBuffer(if (static) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC)
            quadBuffer = ShaderStorageBuffer(if (static) ShaderStorageBuffer.Usage.STATIC else ShaderStorageBuffer.Usage.STREAM)
        }
    }

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

    fun fullRebuild(manager: LightManager, box: BlockBox, center: Vector3f, radius: Float) {
        fullRebuildTask?.cancel(true)
        fullRebuildTask = CompletableFuture.supplyAsync {
            val centerBlock = BlockPos.containing(Vec3(center))
            val radiusSq = radius * radius
            val volumes = LinkedList<ShadowVolume>()

            for (x in box.min.x..box.max.x) {
                for (y in box.min.y..box.max.y) {
                    for (z in box.min.z..box.max.z) {
                        val pos = BlockPos(x, y, z)

                        if (pos != centerBlock && pos.distToCenterSqr(Vec3(center)) <= radiusSq) {
                            getLightFaces(
                                manager.getLevel(),
                                centerBlock,
                                pos
                            ) { face ->
                                volumes.add(face.toVolumePoint(center, radius))
                            }
                        }
                    }
                }
            }

            return@supplyAsync volumes
        }
    }

    private fun uploadShadows(lightPos: BlockPos) {
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
                val debugBuilder =
                    Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR)

                for (shadow in shadows) {
                    shadow.buildDebug(lightPos, debugBuilder)
                }

                val debugBuilt = debugBuilder.build()!!

                it.bind()
                it.upload(debugBuilt)
                VertexBuffer.unbind()
            }
        }
    }

    fun render(manager: LightManager, raytrace: Boolean, pos: Vector3f) {
        if (fullRebuildTask?.isDone ?: false) {
            shadows = fullRebuildTask!!.get()
            fullRebuildTask = null
            uploadShadows(BlockPos.containing(Vec3(pos)))
        }

        val renderType = VeilRenderType.get(Vibrancy.id("point_shadow"))!!
        renderType.setupRenderState()

        GL11.glClear(GL_COLOR_BUFFER_BIT)

        if (raytrace && shadows.isNotEmpty()) {
            val shader = RenderSystem.getShader()!!

            shader.safeGetUniform("LightPos").set(pos)
            shader.setSampler("AtlasSampler", Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS))

            quadBuffer!!.bindBase(0)

            shadowMesh!!.bind()
            shadowMesh!!.drawWithShader(
                manager.viewMatrix!!,
                RenderSystem.getProjectionMatrix(),
                shader
            )
            VertexBuffer.unbind()

            ShaderStorageBuffer.unbindBase(0)
        }

        renderType.clearRenderState()

        if (raytrace && shadows.isNotEmpty() && Vibrancy.RENDER_DEBUG_LINES) {
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