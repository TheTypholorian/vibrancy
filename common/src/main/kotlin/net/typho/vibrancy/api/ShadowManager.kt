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
import net.minecraft.world.phys.Vec3
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.api.LightFace.Companion.toLightFace
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class ShadowManager(
    static: Boolean
) : NativeResource {
    val shadowMesh = VertexBuffer(if (static) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC)
    val quadBuffer = ShaderStorageBuffer(if (static) ShaderStorageBuffer.Usage.STATIC else ShaderStorageBuffer.Usage.STREAM)
    private var shadows: List<ShadowVolume> = LinkedList()
    private var fullRebuildTask: CompletableFuture<List<ShadowVolume>>? = null

    override fun free() {
        shadowMesh.close()
        quadBuffer.close()
    }

    fun isTaskActive() = !(fullRebuildTask?.isDone ?: false)

    fun getLightFaces(
        level: ClientLevel,
        pos: BlockPos,
        out: Consumer<LightFace>
    ) {
        val state = level.getBlockState(pos)
        val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
        val random = RandomSource.create()
        val offset = state.getOffset(level, pos)

        for (dir in Direction.entries) {
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

        for (quad in model.getQuads(state, null, random)) {
            out.accept(quad.toLightFace(
                offset.x.toFloat(),
                offset.y.toFloat(),
                offset.z.toFloat(),
                pos,
                null
            ))
        }
    }

    fun fullRebuild(manager: LightManager, box: BlockBox, center: Vector3f, radius: Float) {
        fullRebuildTask?.cancel(true)
        fullRebuildTask = CompletableFuture.supplyAsync {
            val centerBlock = BlockPos.containing(center.x.toDouble(), center.y.toDouble(), center.z.toDouble())
            val radiusSq = radius * radius
            val volumes = LinkedList<ShadowVolume>()

            for (x in box.min.x..box.max.x) {
                for (y in box.min.y..box.max.y) {
                    for (z in box.min.z..box.max.z) {
                        val pos = BlockPos(x, y, z)

                        if (pos != centerBlock && pos.distToCenterSqr(Vec3(center)) <= radiusSq) {
                            getLightFaces(
                                manager.getLevel(),
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

    private fun uploadShadows() {
        if (shadows.isNotEmpty()) {
            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)

            for (shadow in shadows) {
                shadow.buildGeometry(builder)
            }

            val built = builder.build()

            if (built != null) {
                shadowMesh.bind()
                shadowMesh.upload(built)
                VertexBuffer.unbind()

                val buf = MemoryUtil.memAlloc(shadows.size * LightFace.BYTES)

                for (shadow in shadows) {
                    shadow.toLightFace().put(buf)
                }

                quadBuffer.bind()
                quadBuffer.upload(buf.flip())
                ShaderStorageBuffer.unbind()

                MemoryUtil.memFree(buf)
            }
        }
    }

    fun render(manager: LightManager, raytrace: Boolean, pos: Vector3f) {
        if (fullRebuildTask?.isDone ?: false) {
            shadows = fullRebuildTask!!.get()
            fullRebuildTask = null
            uploadShadows()
        }

        val renderType = VeilRenderType.get(Vibrancy.id("point_shadow"))!!
        renderType.setupRenderState()

        GL11.glClear(GL_COLOR_BUFFER_BIT)

        if (raytrace && shadows.isNotEmpty()) {
            val shader = RenderSystem.getShader()!!

            shader.safeGetUniform("LightPos").set(pos)
            shader.setSampler("AtlasSampler", Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS))

            quadBuffer.bindBase(0)

            shadowMesh.bind()
            shadowMesh.drawWithShader(
                manager.viewMatrix!!,
                RenderSystem.getProjectionMatrix(),
                shader
            )
            VertexBuffer.unbind()

            ShaderStorageBuffer.unbindBase(0)
        }

        renderType.clearRenderState()
    }
}