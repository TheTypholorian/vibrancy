package net.typho.vibrancy

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.VertexBuffer
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoFramebuffer
import net.typho.big_shot_lib.gl.resource.TextureFormat
import net.typho.big_shot_lib.spirv.ShaderMixinManager
import net.typho.vibrancy.block.BlockLightRegistry
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
    val config = ConfigApi.registerAndLoadConfig(::VibrancyConfig, RegisterType.CLIENT)

    @JvmField
    val LIGHT_MANAGER = LightManager()
    val OUTPUT_FBO: NeoFramebuffer by lazy {
        val fbo = object : NeoFramebuffer.TextureBacked(
            id("output"),
            arrayOf(TextureFormat.RGB16F),
            TextureFormat.DEPTH24_STENCIL8,
            Minecraft.getInstance().window.width / config.downscale.factor.get(),
            Minecraft.getInstance().window.height / config.downscale.factor.get()
        ) {
            override fun resize(width: Int, height: Int) {
                super.resize(width / config.downscale.factor.get(), height / config.downscale.factor.get())
            }
        }
        NeoFramebuffer.AUTO_RESIZE.add(fbo)
        NeoFramebuffer.register(fbo)
        fbo.colorAttachments.forEach { attachment ->
            if (attachment is ITexture) {
                attachment.setInterpolation(config.downscale.interpolation.get().inner)
            }
        }
        fbo
    }
    val WORLD_POS_FBO by lazy {
        val fbo = NeoFramebuffer.TextureBacked(
            id("world_pos"),
            arrayOf(TextureFormat.RGB32F),
            null,
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

    @JvmStatic
    fun init() {
        ShaderMixinManager.register(VibrancyDynamicBuffers)
        BlockLightRegistry.init()
    }

    @JvmStatic
    fun render() {
        WORLD_POS_FBO.bind()

        LIGHT_MANAGER.blitWorldPos()

        WORLD_POS_FBO.unbind()

        OUTPUT_FBO.bind()

        GlStateManager._viewport(0, 0, OUTPUT_FBO.width(), OUTPUT_FBO.height())

        GlStateManager._clearColor(0f, 0f, 0f, 0f)
        GlStateManager._clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT, false)

        LIGHT_MANAGER.render()

        OUTPUT_FBO.unbind()
        VertexBuffer.unbind()

        Minecraft.getInstance().mainRenderTarget.bindWrite(true)

        LIGHT_MANAGER.blitOutput(OUTPUT_FBO.colorAttachments[0] as ITexture)

        Minecraft.getInstance().mainRenderTarget.bindWrite(false)

        LIGHT_MANAGER.renderDebug()
    }

    @JvmStatic
    fun addDebugInfo(out: Consumer<String>) {
        out.accept(ChatFormatting.UNDERLINE.toString() + MOD_NAME)

        LIGHT_MANAGER.getDebugOutput(out)
    }

    @JvmStatic
    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}