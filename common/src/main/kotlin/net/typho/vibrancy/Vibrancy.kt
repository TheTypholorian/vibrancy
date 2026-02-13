package net.typho.vibrancy

import com.mojang.blaze3d.platform.GlStateManager
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.typho.big_shot_lib.api.client.rendering.event.PostProcessEvent
import net.typho.big_shot_lib.api.client.rendering.event.RenderData
import net.typho.big_shot_lib.api.client.rendering.event.WindowResizeEvent
import net.typho.big_shot_lib.api.client.rendering.shaders.mixins.ShaderMixinManager
import net.typho.big_shot_lib.api.client.rendering.textures.GlTexture
import net.typho.big_shot_lib.api.client.rendering.textures.NeoFramebuffer
import net.typho.big_shot_lib.api.client.rendering.textures.NeoTexture2D
import net.typho.big_shot_lib.api.client.rendering.textures.TextureFormat
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.block.BlockLightRegistry
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Vibrancy {
    const val MOD_ID = "vibrancy"
    const val MOD_NAME = "Vibrancy"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

    @JvmField
    val config = ConfigApi.registerAndLoadConfig(::VibrancyConfig, RegisterType.CLIENT)

    @JvmField
    val LIGHT_MANAGER = LightManager()
    val OUTPUT_FBO by lazy {
        NeoFramebuffer(
            listOf(NeoTexture2D(TextureFormat.RGB16F)),
            null,
            Minecraft.getInstance().mainRenderTarget.width,
            Minecraft.getInstance().mainRenderTarget.height
        )
    }
    val WORLD_POS_FBO by lazy {
        NeoFramebuffer(
            listOf(NeoTexture2D(TextureFormat.RGB32F)),
            null,
            Minecraft.getInstance().mainRenderTarget.width,
            Minecraft.getInstance().mainRenderTarget.height
        )
    }
    @JvmField
    var iProjMat = Matrix4f()
    @JvmField
    var iModelMat = Matrix4f()
    @JvmField
    var camera = Vector3f()

    @JvmStatic
    fun init() {
        ShaderMixinManager.register(VibrancyDynamicBuffers)
        BlockLightRegistry.init()
        WindowResizeEvent.register { width, height ->
            OUTPUT_FBO.resize(width, height)
            WORLD_POS_FBO.resize(width, height)
        }
        PostProcessEvent.register(this::render)
    }

    @JvmStatic
    fun render(data: RenderData) {
        WORLD_POS_FBO.bind()

        LIGHT_MANAGER.blitWorldPos(data)

        WORLD_POS_FBO.unbind()

        OUTPUT_FBO.bind()

        GlStateManager._viewport(0, 0, OUTPUT_FBO.width(), OUTPUT_FBO.height())

        GlStateManager._clearColor(0f, 0f, 0f, 0f)
        GlStateManager._clear(GL_COLOR_BUFFER_BIT, false)

        LIGHT_MANAGER.render(data, OUTPUT_FBO)

        OUTPUT_FBO.unbind()

        Minecraft.getInstance().mainRenderTarget.bindWrite(true)

        LIGHT_MANAGER.blitOutput(data, OUTPUT_FBO.colorAttachments[0] as GlTexture)

        Minecraft.getInstance().mainRenderTarget.bindWrite(false)
    }

    @JvmStatic
    fun addDebugInfo(out: Consumer<String>) {
        out.accept(ChatFormatting.UNDERLINE.toString() + MOD_NAME)

        LIGHT_MANAGER.getDebugOutput(out)
    }

    @JvmStatic
    fun id(path: String): ResourceIdentifier = ResourceIdentifier(MOD_ID, path)
}