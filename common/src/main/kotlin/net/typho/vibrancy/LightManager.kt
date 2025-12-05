package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import foundry.veil.api.client.render.VeilRenderSystem
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.GlobalPos
import net.typho.vibrancy.api.Light
import net.typho.vibrancy.api.invert
import org.joml.Matrix4f

open class LightManager(
    var dirtyBlocks: Iterable<GlobalPos>,

    var maxRendered: Int,
    var maxRaytraced: Int
) {
    companion object {
        const val SKY_STENCIL_MASK: Int = 1 shl 6
        const val BLOCK_STENCIL_MASK: Int = 1 shl 7
    }

    var lightsRendered: Int = 0
    var lightsRaytraced: Int = 0

    fun getWorld(): ClientLevel = Minecraft.getInstance().level!!

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun createViewMatrix(camera: Camera = getCamera()): Matrix4f = RenderSystem.getModelViewMatrix()
        .translate(camera.position.toVector3f().invert())

    fun shouldRender(light: Light): Boolean {
        if (lightsRendered >= maxRendered) {
            return false
        }

        val box = light.getCullingBox()

        if (box != null) {
            return VeilRenderSystem.getCullingFrustum().testAab(box)
        }

        return true
    }

    fun postRender(light: Light, didRaytrace: Boolean) {
        lightsRendered++

        if (didRaytrace) {
            lightsRaytraced++
        }
    }
}