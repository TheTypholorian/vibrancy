package net.typho.vibrancy

import foundry.veil.api.client.render.VeilRenderSystem
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType
import foundry.veil.platform.VeilEventPlatform
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.typho.vibrancy.api.glClear
import net.typho.vibrancy.block.BlockLight
import org.lwjgl.opengl.GL11.*
import java.util.*

object Vibrancy {
    const val MOD_ID = "vibrancy"

    val dirtyBlocks = LinkedList<GlobalPos>()
    val lightManager = LightManager(dirtyBlocks, 20, 10)

    fun init() {
        ModRenderTypeLayers.init()
        VeilEventPlatform.INSTANCE.onVeilRendererAvailable { renderer ->
            renderer.postProcessingManager.add(id("post"))
        }
    }

    fun render() {
        VeilRenderSystem.renderer().enableBuffers(id("light"), DynamicBufferType.NORMAL, DynamicBufferType.ALBEDO, DynamicBufferType.LIGHT_UV)

        VeilRenderSystem.renderer().framebufferManager.getFramebuffer(id("output"))?.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        VeilRenderSystem.renderer().framebufferManager.getFramebuffer(id("shadows"))?.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

        for (light in BlockLight.LIGHTS.values) {
            light.render(lightManager)
        }
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}