package net.typho.vibrancy.api

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import foundry.veil.api.client.render.VeilRenderSystem
import foundry.veil.api.client.render.rendertype.VeilRenderType
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.typho.vibrancy.Vibrancy
import org.joml.Matrix4f

open class LightManager(
    var dirtyBlocks: Iterable<GlobalPos>,

    var raytraceDistance: Int,
    var lightCullDistance: Int,

    var maxRendered: Int,
    var maxRaytraced: Int,

    var shadowRadius: Int
) {
    companion object {
        const val SKY_STENCIL_MASK: Int = 0b01000000
        const val BLOCK_STENCIL_MASK: Int = 0b10000000
        const val SHADOW_MASK: Int = 0b1

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

    fun tickDelta(): Float = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(false)

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun createViewMatrix(camera: Camera = getCamera()): Matrix4f = RenderSystem.getModelViewMatrix()
        .translate(camera.position.toVector3f().invert())

    fun setupStencil(framebuffer: ResourceLocation) {
        val block = VeilRenderType.get(Vibrancy.id("stencil_setup_block"), framebuffer.toString())!!
        block.setupRenderState()

        SCREEN_VBO!!.bind()
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        SCREEN_VBO!!.drawWithShader(null, null, RenderSystem.getShader()!!)
        VertexBuffer.unbind()

        block.clearRenderState()
    }

    fun shouldRender(light: Light, camera: Camera = getCamera()): Boolean {
        return (maxRendered > 400 || lightsRendered < maxRendered)
                && light.testCullingDistance(camera, lightCullDistance)
                && VeilRenderSystem.getCullingFrustum().testAab(light.getCullingBox())
    }

    fun shouldRaytrace(light: Light, camera: Camera = getCamera()): Boolean {
        return (maxRendered > 400 || lightsRaytraced < maxRaytraced)
                && light.testCullingDistance(camera, raytraceDistance)
    }

    fun postRender(light: Light, didRaytrace: Boolean) {
        lightsRendered++

        if (didRaytrace) {
            lightsRaytraced++
        }
    }
}