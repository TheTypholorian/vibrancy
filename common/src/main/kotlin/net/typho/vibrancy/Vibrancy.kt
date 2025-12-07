package net.typho.vibrancy

import foundry.veil.api.client.render.VeilRenderSystem
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType
import foundry.veil.platform.VeilEventPlatform
import net.minecraft.ChatFormatting
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.typho.vibrancy.api.BlockLight
import net.typho.vibrancy.api.LightManager
import net.typho.vibrancy.api.glClear
import org.lwjgl.opengl.GL11.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

object Vibrancy {
    const val MOD_ID = "vibrancy"
    const val MOD_NAME = "Vibrancy"
    @JvmStatic
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

    val dirtyBlocks = LinkedList<GlobalPos>()
    val lightManager = LightManager(dirtyBlocks, 200, 10)

    fun init() {
        ModRenderTypeLayers.init()
        VeilEventPlatform.INSTANCE.onVeilRendererAvailable { renderer ->
            renderer.postProcessingManager.add(id("post"))
        }
    }

    fun addDebugInfo(out: Consumer<String>) {
        out.accept("")
        out.accept(ChatFormatting.UNDERLINE.toString() + "Vibrancy")

        out.accept("Block Lights")
        out.accept("${BlockLight.LIGHTS.size} lights in world")
        out.accept("${lightManager.lightsRendered}/${lightManager.maxRendered} rendered")
        out.accept("${lightManager.lightsRaytraced}/${lightManager.maxRaytraced} raytraced")
    }

    fun render() {
        lightManager.viewMatrix = lightManager.createViewMatrix()
        lightManager.lightsRendered = 0
        lightManager.lightsRaytraced = 0

        VeilRenderSystem.renderer()
            .enableBuffers(id("light"), DynamicBufferType.NORMAL, DynamicBufferType.ALBEDO, DynamicBufferType.LIGHT_UV)

        val outputFramebuffer = VeilRenderSystem.renderer().framebufferManager.getFramebuffer(id("output"))!!
        val shadowsFramebuffer = VeilRenderSystem.renderer().framebufferManager.getFramebuffer(id("shadows"))!!

        outputFramebuffer.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        shadowsFramebuffer.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

        lightManager.setupStencil(id("shadows"))

        BlockLight.LIGHTS.values.stream()
            .sorted(Comparator.comparingDouble {
                it.getPosition().distanceSquared(lightManager.getCamera().position.toVector3f()).toDouble()
            })
            .forEachOrdered { light ->
                if (lightManager.shouldRender(light)) {
                    val raytrace = lightManager.shouldRaytrace(light)

                    light.render(lightManager, raytrace)
                    lightManager.postRender(light, raytrace)
                }
            }
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}