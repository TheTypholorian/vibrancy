package net.typho.vibrancy

import foundry.veil.api.client.render.VeilRenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.GlobalPos
import net.typho.vibrancy.api.Light

class LightManager {
    var dirtyBlocks: Iterable<GlobalPos>? = null

    val skyStencilMask: Int = 1 shl 6
    val blockStencilMask: Int = 1 shl 7

    val maxRendered: Int = 10
    val maxRaytraced: Int = 10

    var lightsRendered: Int = 0
    var lightsRaytraced: Int = 0

    fun getWorld(): ClientLevel = Minecraft.getInstance().level!!

    fun shouldRender(light: Light): Boolean {
        return lightsRendered < maxRendered && VeilRenderSystem.getCullingFrustum().testAab(light.getCullingBoundingBox())
    }

    fun postRender(light: Light, didRaytrace: Boolean) {
        lightsRendered++

        if (didRaytrace) {
            lightsRaytraced++
        }
    }
}