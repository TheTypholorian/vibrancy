package net.typho.vibrancy

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.VertexBuffer
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoFramebuffer
import net.typho.big_shot_lib.gl.resource.TextureFormat
import net.typho.big_shot_lib.spirv.ShaderMixinCallback
import net.typho.vibrancy.old.BlockLight
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Vibrancy {
    const val MOD_ID = "vibrancy"
    const val MOD_NAME = "Vibrancy"

    @JvmField
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

    @JvmStatic
    fun init() {
        ShaderMixinCallback.register(VibrancyDynamicBuffers)
    }

    @JvmStatic
    fun render() {
        OUTPUT_FBO.bind()

        GlStateManager._clearColor(0f, 0f, 0f, 0f)
        GlStateManager._clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT, false)

        LIGHT_MANAGER.render()

        OUTPUT_FBO.unbind()
        VertexBuffer.unbind()

        LIGHT_MANAGER.blitOutput(OUTPUT_FBO.colorAttachments[0] as ITexture)
    }

    @JvmStatic
    fun getRenderTypeTexture(renderType: RenderType): ResourceLocation {
        return when (renderType) {
            is RenderType.CompositeRenderType -> {
                renderType.state().textureState.cutoutTexture().orElseThrow()
            }

            else -> throw UnsupportedOperationException("Unable to get texture for render type ${renderType.javaClass} $renderType")
        }
    }

    @JvmStatic
    fun pointsToward(face: Direction, offset: Vector3f): Boolean {
        val normal = face.normal
        return Vector3f(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).dot(offset) > 0
    }

    @JvmStatic
    fun addDebugInfo(out: Consumer<String>) {
        out.accept(ChatFormatting.UNDERLINE.toString() + "Vibrancy")

        LIGHT_MANAGER.getDebugOutput(out)
    }

    @JvmStatic
    fun reloadShadows() {
        for (light in BlockLight.LIGHTS.values) {
            light.shadowsDirty = true
        }
    }

    @JvmStatic
    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}