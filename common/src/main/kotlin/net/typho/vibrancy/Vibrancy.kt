package net.typho.vibrancy

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
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
import net.typho.vibrancy.platform.Services
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import kotlin.use

object Vibrancy {
    const val MOD_ID = "vibrancy"
    const val MOD_NAME = "Vibrancy"
    const val CONFIG_FILE_NAME = "$MOD_ID.json"

    val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

    val DIRTY_BLOCKS = LinkedList<GlobalPos>()
    var LIGHT_BRIGHTNESS: Float = 1f
    var ENTITY_SHADOWS: Boolean = true
    val LIGHT_MANAGER = LightManager(DIRTY_BLOCKS, 16, 32, 200, 100, 6)
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

    fun init() {
        loadConfig()
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
                        LIGHT_MANAGER.postRender(light, raytrace)
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

            DIRTY_BLOCKS.clear()
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
        val tasks = BlockLight.LIGHTS.values.stream()
            .mapToInt { if (it.shadows.isTaskActive()) 1 else 0 }
            .sum()
        out.accept("$tasks async tasks")
        val entities = BlockLight.LIGHTS.values.stream()
            .mapToInt { it.shadows.numEntities() }
            .sum()
        out.accept("$entities entity shadows")
        val blockEntities = BlockLight.LIGHTS.values.stream()
            .mapToInt { it.shadows.numBlockEntities() }
            .sum()
        out.accept("$blockEntities block entity shadows")
    }

    fun reloadShadows() {
        for (light in BlockLight.LIGHTS.values) {
            light.shadowsDirty = true
        }
    }

    fun getConfigFile(): Path {
        val path = Services.PLATFORM.getConfigDir().resolve(CONFIG_FILE_NAME)

        if (Files.notExists(path)) {
            Files.createFile(path)
            Minecraft.getInstance().options.entityShadows().set(false)
        }

        return path
    }

    fun loadConfig() {
        Files.newBufferedReader(getConfigFile()).use { reader ->
            val jsonElement = JsonParser.parseReader(reader)

            if (!jsonElement.isJsonObject) {
                return@use
            }

            val json = jsonElement.asJsonObject

            json.get("raytraceDistance")?.let { LIGHT_MANAGER.raytraceDistance = it.asInt }
            json.get("lightCullDistance")?.let { LIGHT_MANAGER.lightCullDistance = it.asInt }
            json.get("maxRendered")?.let { LIGHT_MANAGER.maxRendered = it.asInt }
            json.get("maxRaytraced")?.let { LIGHT_MANAGER.maxRaytraced = it.asInt }
            json.get("shadowRadius")?.let { LIGHT_MANAGER.shadowRadius = it.asInt }
            json.get("lightBrightness")?.let { LIGHT_BRIGHTNESS = it.asFloat }
            json.get("entityShadows")?.let { ENTITY_SHADOWS = it.asBoolean }
        }
    }

    fun saveConfig() {
        Files.newBufferedWriter(getConfigFile()).use { writer ->
            val json = JsonObject()

            json.addProperty("raytraceDistance", LIGHT_MANAGER.raytraceDistance)
            json.addProperty("lightCullDistance", LIGHT_MANAGER.lightCullDistance)
            json.addProperty("maxRendered", LIGHT_MANAGER.maxRendered)
            json.addProperty("maxRaytraced", LIGHT_MANAGER.maxRaytraced)
            json.addProperty("shadowRadius", LIGHT_MANAGER.shadowRadius)
            json.addProperty("lightBrightness", LIGHT_BRIGHTNESS)
            json.addProperty("entityShadows", ENTITY_SHADOWS)

            writer.write(GsonBuilder().setPrettyPrinting().create().toJson(json))
        }
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}