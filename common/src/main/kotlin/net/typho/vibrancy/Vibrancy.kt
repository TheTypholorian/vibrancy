package net.typho.vibrancy

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.NeoFramebuffer
import net.typho.big_shot_lib.api.NeoShader
import net.typho.big_shot_lib.gl.GlResourceType
import net.typho.big_shot_lib.gl.TextureFormat
import net.typho.vibrancy.light.BlockLight
import net.typho.vibrancy.light.LightManager
import net.typho.vibrancy.platform.Services
import net.typho.vibrancy.util.glClear
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer

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

    var PATCHES_MODE: VertexFormat.Mode? = null

    fun init() {
        loadConfig()
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

    fun render() {
        LIGHT_MANAGER.viewMatrix = LIGHT_MANAGER.getViewMatrix()
        LIGHT_MANAGER.lightsRendered = 0
        LIGHT_MANAGER.lightsRaytraced = 0

        OUTPUT_FBO.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        OUTPUT_FBO.bind().use {
            LIGHT_MANAGER.setupStencil(id("output"))

            glColorMask(true, true, true, true)
            glDisable(GL_DEPTH_TEST)
            glCullFace(GL_FRONT)
            glEnable(GL_BLEND)
            glBlendFunc(GL_ONE, GL_ONE)
            glEnable(GL_STENCIL_TEST)
            glStencilMask(1)
            glStencilFunc(GL_ALWAYS, 0, 0xFF)
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)

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

            GlResourceType.SHADER_STORAGE_BUFFER.unbindBase(0)
            VertexBuffer.unbind()

            glDisable(GL_STENCIL_TEST)

            DIRTY_BLOCKS.clear()
        }

        // TODO albedo
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        NeoShader.get(id("post"))!!.bind().use {
            val shader = it.resource()
            shader.setCommonUniforms()
            shader.setSampler("VibrancyOutputSampler", OUTPUT_FBO.colorAttachments[0] as ITexture)

            BigShotLib.SCREEN_VBO.bind()
            BigShotLib.SCREEN_VBO.draw()
            VertexBuffer.unbind()
        }

        glCullFace(GL_BACK)
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