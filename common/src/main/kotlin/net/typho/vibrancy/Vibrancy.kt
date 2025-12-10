package net.typho.vibrancy

import com.mojang.blaze3d.vertex.VertexBuffer
import foundry.veil.api.client.render.VeilRenderSystem
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType
import foundry.veil.api.client.render.rendertype.VeilRenderType
import foundry.veil.platform.VeilEventPlatform
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.typho.vibrancy.api.BlockLight
import net.typho.vibrancy.api.LightManager
import net.typho.vibrancy.api.ShaderStorageBuffer
import net.typho.vibrancy.api.glClear
import org.joml.Vector3f
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

    val DIRTY_BLOCKS = LinkedList<GlobalPos>()
    val LIGHT_MANAGER = LightManager(DIRTY_BLOCKS, 8, 32, 200, 100, 8)

    var RENDER_DEBUG_LINES = false

    fun init() {
        ModRenderTypeLayers.init()
        VeilEventPlatform.INSTANCE.onVeilRendererAvailable { renderer ->
            renderer.postProcessingManager.add(id("post"))
        }
    }

    fun pointsToward(face: Direction, offset: Vector3f?): Boolean {
        val normal = face.normal
        return Vector3f(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).dot(offset) > 0
    }

    fun addDebugInfo(out: Consumer<String>) {
        out.accept("")
        out.accept(ChatFormatting.UNDERLINE.toString() + "Vibrancy")

        out.accept("Block Lights")
        out.accept("${BlockLight.LIGHTS.size} lights in world")
        out.accept("${LIGHT_MANAGER.lightsRendered}/${LIGHT_MANAGER.maxRendered} rendered")
        out.accept("${LIGHT_MANAGER.lightsRaytraced}/${LIGHT_MANAGER.maxRaytraced} raytraced")

        val shadows = BlockLight.LIGHTS.values.stream()
            .mapToInt { it.shadows.numShadows() }
            .sum()
        out.accept("$shadows shadows")
        val quads = BlockLight.LIGHTS.values.stream()
            .mapToInt { it.shadows.numQuads() }
            .sum()
        out.accept("$quads quads")
    }

    fun render() {
        LIGHT_MANAGER.viewMatrix = LIGHT_MANAGER.createViewMatrix()
        LIGHT_MANAGER.lightsRendered = 0
        LIGHT_MANAGER.lightsRaytraced = 0

        VeilRenderSystem.renderer()
            .enableBuffers(id("light"), DynamicBufferType.NORMAL, DynamicBufferType.ALBEDO, DynamicBufferType.LIGHT_UV)

        val outputFramebuffer = VeilRenderSystem.renderer().framebufferManager.getFramebuffer(id("output"))!!

        outputFramebuffer.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

        LIGHT_MANAGER.setupStencil(id("output"))

        val pointRenderType = VeilRenderType.get(id("point_common"))!!
        pointRenderType.setupRenderState()

        BlockLight.LIGHTS.values.stream()
            .sorted(Comparator.comparingDouble {
                it.getPosition().distanceSquared(LIGHT_MANAGER.getCamera().position.toVector3f()).toDouble()
            })
            .forEachOrdered { light ->
                if (LIGHT_MANAGER.shouldRender(light)) {
                    val raytrace = LIGHT_MANAGER.shouldRaytrace(light)

                    light.render(LIGHT_MANAGER, raytrace)
                    LIGHT_MANAGER.postRender(light, raytrace)
                }
            }

        pointRenderType.clearRenderState()

        ShaderStorageBuffer.unbindBase(0)
        VertexBuffer.unbind()

        DIRTY_BLOCKS.clear()
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}