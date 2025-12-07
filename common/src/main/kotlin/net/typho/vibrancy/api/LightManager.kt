package net.typho.vibrancy.api

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import foundry.veil.api.client.render.rendertype.VeilRenderType
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.Vibrancy
import org.joml.Matrix4f

open class LightManager(
    var dirtyBlocks: Iterable<GlobalPos>,

    var maxRendered: Int,
    var maxRaytraced: Int
) {
    companion object {
        const val SKY_STENCIL_MASK: Int = 0b01000000
        const val BLOCK_STENCIL_MASK: Int = 0b10000000

        var SCREEN_VBO: VertexBuffer? = null

        init {
            RenderSystem.recordRenderCall {
                val builder = RenderSystem.renderThreadTesselator()
                    .begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX)
                builder.addVertex(-1f, 1f, 0f).setUv(0f, 1f)
                builder.addVertex(-1f, -1f, 0f).setUv(0f, 0f)
                builder.addVertex(1f, 1f, 0f).setUv(1f, 1f)
                builder.addVertex(1f, -1f, 0f).setUv(1f, 0f)

                SCREEN_VBO = VertexBuffer(VertexBuffer.Usage.STATIC)
                SCREEN_VBO!!.bind()
                SCREEN_VBO!!.upload(builder.buildOrThrow())
                VertexBuffer.unbind()
            }
        }
    }

    var lightsRendered: Int = 0
    var lightsRaytraced: Int = 0
    var viewMatrix: Matrix4f? = null

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun createViewMatrix(camera: Camera = getCamera()): Matrix4f = RenderSystem.getModelViewMatrix()
        .translate(camera.position.toVector3f().invert())

    fun createOcclusionMask(level: Level, pos: BlockPos, state: BlockState): Int {
        var mask = 0

        for (dir in Direction.entries) {
            if (Block.shouldRenderFace(state, level, pos, dir, pos.relative(dir))) {
                mask = mask or (1 shl dir.ordinal)
            }
        }

        return mask
    }

    fun setupStencil(framebuffer: ResourceLocation) {
        val block = VeilRenderType.get(Vibrancy.id("stencil_setup_block"), framebuffer.toString())!!
        block.setupRenderState()

        SCREEN_VBO!!.bind()
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        SCREEN_VBO!!.drawWithShader(null, null, RenderSystem.getShader()!!)
        VertexBuffer.unbind()

        block.clearRenderState()
    }

    fun shouldRender(light: Light): Boolean {
        return lightsRendered < maxRendered
    }

    fun shouldRaytrace(light: Light): Boolean {
        return lightsRaytraced < maxRaytraced
    }

    fun postRender(light: Light, didRaytrace: Boolean) {
        lightsRendered++

        if (didRaytrace) {
            lightsRaytraced++
        }
    }
}