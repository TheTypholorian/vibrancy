package net.typho.vibrancy

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.VertexBuffer
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoFramebuffer
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.big_shot_lib.gl.resource.TextureFormat
import net.typho.big_shot_lib.gl.state.*
import net.typho.big_shot_lib.spirv.ShaderMixinCallback
import net.typho.vibrancy.light.BlockLight
import net.typho.vibrancy.light.LightManager
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Vibrancy {
    const val MOD_ID = "vibrancy"
    const val MOD_NAME = "Vibrancy"

    val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

    @JvmField
    val LIGHT_MANAGER = LightManager()
    val OUTPUT_FBO by lazy {
        val fbo = NeoFramebuffer.TextureBacked(
            id("output"),
            arrayOf(TextureFormat.RGB16F),
            TextureFormat.DEPTH24_STENCIL8,
            Minecraft.getInstance().window.width,
            Minecraft.getInstance().window.height
        )
        NeoFramebuffer.AUTO_RESIZE.add(fbo)
        NeoFramebuffer.register(fbo)
        fbo
    }
    @JvmField
    var iProjMat = Matrix4f()
    @JvmField
    var iModelMat = Matrix4f()
    @JvmField
    var camera = Vector3f()
    @JvmField
    val config = ConfigApi.registerAndLoadConfig(::VibrancyConfig)

    fun init() {
        ShaderMixinCallback.register(VibrancyDynamicBuffers)
    }

    fun render() {
        LIGHT_MANAGER.viewMatrix = Matrix4f(LIGHT_MANAGER.getViewMatrix())
        LIGHT_MANAGER.lightsRendered = 0
        LIGHT_MANAGER.lightsRaytraced = 0

        GlStack().use { stack ->
            OUTPUT_FBO.bind(stack)

            GlStateManager._clearColor(0f, 0f, 0f, 0f)
            GlStateManager._clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT, false)

            LIGHT_MANAGER.setupStencil(stack)

            stack.set(ColorMask(true, true, true, true))
            stack.set(DepthMask, false)
            stack.disable(GlCapability.DEPTH_TEST)
            stack.enable(GlCapability.CULL_FACE)
            stack.set(CullFace.FRONT)
            stack.enable(GlCapability.BLEND)
            stack.set(
                BlendFunction(
                    BlendFactor.ONE,
                    BlendFactor.ONE
                )
            )
            stack.set(BlendEquation.ADD)
            stack.set(StencilMask, 1)
            stack.set(
                StencilFunc(
                    ComparisonMode.ALWAYS,
                    0,
                    0xFF
                )
            )
            stack.set(
                StencilOp(
                    IntAction.KEEP,
                    IntAction.KEEP,
                    IntAction.KEEP
                )
            )

            BlockLight.LIGHTS.values.stream()
                .sorted(Comparator.comparingDouble {
                    it.getPosition().distanceSquared(LIGHT_MANAGER.getCamera().position.toVector3f()).toDouble()
                })
                .forEachOrdered { light ->
                    if (LIGHT_MANAGER.shouldRender(light)) {
                        val raytrace = LIGHT_MANAGER.shouldRaytrace(light)
                        light.render(LIGHT_MANAGER, raytrace, stack)
                        LIGHT_MANAGER.postRenderLight(raytrace)
                    }
                }

            stack.boundMap[GlResourceType.FRAMEBUFFER]?.unbind()

            stack.disable(GlCapability.CULL_FACE)
            stack.disable(GlCapability.BLEND)
            val shader = NeoShader.get(id("post"))!!
            shader.bind(stack)
            shader.setCommonUniforms()
            shader.setSampler("DiffuseSampler0", Minecraft.getInstance().mainRenderTarget.colorTextureId)
            shader.setSampler("VibrancyOutputSampler", OUTPUT_FBO.colorAttachments[0] as ITexture)
            shader.setSampler("VibrancyNormalSampler", VibrancyDynamicBuffers.normalsTexture!!)
            shader.setSampler("VibrancyAlbedoSampler", VibrancyDynamicBuffers.albedoTexture!!)

            BigShotLib.SCREEN_VBO.bind()
            BigShotLib.SCREEN_VBO.draw()

            LIGHT_MANAGER.postRender()
        }

        VertexBuffer.unbind()
    }

    fun getRenderTypeTexture(renderType: RenderType): ResourceLocation {
        return when (renderType) {
            is RenderType.CompositeRenderType -> {
                renderType.state().textureState.cutoutTexture().orElseThrow()
            }

            else -> throw UnsupportedOperationException("Unable to get texture for render type ${renderType.javaClass} $renderType")
        }
    }

    fun pointsToward(face: Direction, offset: Vector3f): Boolean {
        val normal = face.normal
        return Vector3f(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).dot(offset) > 0
    }

    fun addDebugInfo(out: Consumer<String>) {
        out.accept("")
        out.accept(ChatFormatting.UNDERLINE.toString() + "Vibrancy")

        out.accept("Block Lights")
        out.accept("${BlockLight.LIGHTS.size} lights in world")
        out.accept("${LIGHT_MANAGER.lightsRendered}/${config.blockLights.maxRendered} rendered")
        out.accept("${LIGHT_MANAGER.lightsRaytraced}/${config.blockLights.maxRaytraced} raytraced")

        val shadows = BlockLight.LIGHTS.values.stream()
            .filter { LIGHT_MANAGER.inRenderDistance(it) }
            .mapToInt { it.shadows.numShadows() }
            .sum()
        out.accept("$shadows shadows")
        val tasks = BlockLight.LIGHTS.values.stream()
            .filter { LIGHT_MANAGER.inRenderDistance(it) }
            .mapToInt { if (it.shadows.isTaskActive()) 1 else 0 }
            .sum()
        out.accept("$tasks async tasks")
        val entities = BlockLight.LIGHTS.values.stream()
            .filter { LIGHT_MANAGER.inRenderDistance(it) }
            .mapToInt { it.shadows.numEntities() }
            .sum()
        out.accept("$entities entity shadows")
        val blockEntities = BlockLight.LIGHTS.values.stream()
            .filter { LIGHT_MANAGER.inRenderDistance(it) }
            .mapToInt { it.shadows.numBlockEntities() }
            .sum()
        out.accept("$blockEntities block entity shadows")
    }

    fun reloadShadows() {
        for (light in BlockLight.LIGHTS.values) {
            light.shadowsDirty = true
        }
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}