package net.typho.vibrancy.light

import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.core.GlobalPos
import net.typho.big_shot_lib.BigShotLib
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.mixin.LevelRendererAccessor
import org.joml.Matrix4f
import java.util.*

open class LightManager {
    companion object {
        const val SHADOW_MASK: Int = 0b1
    }

    @JvmField
    val dirtyBlocks = LinkedList<GlobalPos>()
    @JvmField
    var lightsRendered: Int = 0
    @JvmField
    var lightsRaytraced: Int = 0
    @JvmField
    var viewMatrix: Matrix4f? = null

    fun tickDelta(): Float = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(true)

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun getViewMatrix(camera: Camera = getCamera()): Matrix4f = BigShotLib.getViewMatrix(camera)

    fun getCullingFrustum(): Frustum = (Minecraft.getInstance().levelRenderer as LevelRendererAccessor).cullingFrustum

    fun postRender() {
        dirtyBlocks.clear()
    }

    fun inRenderDistance(light: Light, camera: Camera = getCamera()): Boolean {
        return light.testCullingDistance(camera, Vibrancy.config.blockLights.lightCullDistance.get())
                && (!Vibrancy.config.forNerds.useFrustumCulling || getCullingFrustum().isVisible(light.getCullingBox()!!))
    }

    fun shouldRender(light: Light, camera: Camera = getCamera()): Boolean {
        return lightsRendered < Vibrancy.config.blockLights.maxRendered && inRenderDistance(light, camera)
    }

    fun shouldRaytrace(light: Light, camera: Camera = getCamera()): Boolean {
        return lightsRaytraced < Vibrancy.config.blockLights.maxRendered
                && light.testCullingDistance(camera, Vibrancy.config.blockLights.raytraceDistance.get())
    }

    fun postRenderLight(didRaytrace: Boolean) {
        lightsRendered++

        if (didRaytrace) {
            lightsRaytraced++
        }
    }
}