package net.typho.vibrancy.light

import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.NeoFramebuffer
import net.typho.big_shot_lib.api.NeoShader
import net.typho.vibrancy.Vibrancy
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*

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
    }

    var lightsRendered: Int = 0
    var lightsRaytraced: Int = 0
    var viewMatrix: Matrix4f? = null

    fun tickDelta(): Float = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(false)

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun getViewMatrix(camera: Camera = getCamera()): Matrix4f = BigShotLib.getViewMatrix(camera)

    fun setupStencil(framebuffer: ResourceLocation) {
        NeoShader.get(Vibrancy.id("stencil_setup"))!!.bind().use {
            NeoFramebuffer.get(framebuffer)!!.bind().use {
                glColorMask(false, false, false, false)
                glDepthMask(false)
                glEnable(GL_STENCIL_TEST)
                glStencilMask(BLOCK_STENCIL_MASK)
                glStencilFunc(GL_ALWAYS, BLOCK_STENCIL_MASK, BLOCK_STENCIL_MASK)
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)

                BigShotLib.SCREEN_VBO.bind()
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                BigShotLib.SCREEN_VBO.draw()
                VertexBuffer.unbind()
            }
        }
    }

    fun shouldRender(light: Light, camera: Camera = getCamera()): Boolean {
        return (maxRendered > 400 || lightsRendered < maxRendered)
                && light.testCullingDistance(camera, lightCullDistance)
                // TODO reimplement frustum culling
                //&& VeilRenderSystem.getCullingFrustum().testAab(light.getCullingBox())
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