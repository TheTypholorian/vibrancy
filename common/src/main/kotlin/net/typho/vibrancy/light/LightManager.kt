package net.typho.vibrancy.light

import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.GlobalPos
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.*
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyDynamicBuffers
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
    }

    var lightsRendered: Int = 0
    var lightsRaytraced: Int = 0
    var viewMatrix: Matrix4f? = null

    fun tickDelta(): Float = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(false)

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun getViewMatrix(camera: Camera = getCamera()): Matrix4f = BigShotLib.getViewMatrix(camera)

    fun setupStencil(stack: GlStack) {
        val shader = NeoShader.get(Vibrancy.id("stencil_setup"))!!
        shader.bind(stack)
        shader.setCommonUniforms()
        shader.setSampler("VibrancyLightSampler", VibrancyDynamicBuffers.lightUVTexture!!)

        stack.set(ColorMask(false, false, false, false))
        stack.enable(GlCapability.STENCIL_TEST)
        stack.set(StencilMask, BLOCK_STENCIL_MASK)
        stack.set(
            StencilFunc(
                ComparisonMode.ALWAYS,
                BLOCK_STENCIL_MASK,
                BLOCK_STENCIL_MASK
            )
        )
        stack.set(
            StencilOp(
                IntAction.KEEP,
                IntAction.KEEP,
                IntAction.REPLACE
            )
        )

        BigShotLib.SCREEN_VBO.bind()
        BigShotLib.SCREEN_VBO.draw()
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