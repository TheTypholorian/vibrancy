package net.typho.vibrancy

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import net.minecraft.ChatFormatting
import net.typho.big_shot_lib.api.client.rendering.event.PostProcessEvent
import net.typho.big_shot_lib.api.client.rendering.event.RenderData
import net.typho.big_shot_lib.api.client.rendering.event.WindowResizeEvent
import net.typho.big_shot_lib.api.client.rendering.shaders.mixins.ShaderMixinManager
import net.typho.big_shot_lib.api.client.rendering.textures.*
import net.typho.big_shot_lib.api.util.IColor
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.block.BlockLightRegistry
import org.joml.Matrix4f
import org.joml.Vector3f
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
            GlFramebuffer.MAIN.width(),
            GlFramebuffer.MAIN.height()
        )
    }
    val WORLD_POS_FBO by lazy {
        NeoFramebuffer(
            listOf(NeoTexture2D(TextureFormat.RGB32F)),
            null,
            GlFramebuffer.MAIN.width(),
            GlFramebuffer.MAIN.height()
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

        /*
        if (Services.PLATFORM.isDevelopmentEnvironment()) {
            OpenGL.INSTANCE.addDebugListener(object : OpenGL.DebugListener {
                override fun accept(method: String, vararg args: Any) {
                    LOGGER.info(
                        "$method(${
                            args.joinToString(", ") {
                                when (it) {
                                    is Array<*> -> it.contentDeepToString()
                                    is BooleanArray -> it.contentToString()
                                    is ByteArray -> it.contentToString()
                                    is CharArray -> it.contentToString()
                                    is ShortArray -> it.contentToString()
                                    is IntArray -> it.contentToString()
                                    is LongArray -> it.contentToString()
                                    is FloatArray -> it.contentToString()
                                    is DoubleArray -> it.contentToString()
                                    else -> it.toString()
                                }
                            }
                        })"
                    )
                }
            })
        }
         */
    }

    @JvmStatic
    fun render(data: RenderData) {
        WORLD_POS_FBO.bind()

        LIGHT_MANAGER.blitWorldPos(data)

        WORLD_POS_FBO.unbind()

        OUTPUT_FBO.bind()

        OUTPUT_FBO.viewport()
        OUTPUT_FBO.clear(ClearBit.Color(IColor.BLACK))

        LIGHT_MANAGER.render(data, OUTPUT_FBO)

        OUTPUT_FBO.unbind()

        GlFramebuffer.MAIN.bind()
        GlFramebuffer.MAIN.viewport()

        LIGHT_MANAGER.blitOutput(data, OUTPUT_FBO.colorAttachments[0] as GlTexture)

        GlFramebuffer.MAIN.bind()
    }

    @JvmStatic
    fun addDebugInfo(out: Consumer<String>) {
        out.accept(ChatFormatting.UNDERLINE.toString() + MOD_NAME)

        LIGHT_MANAGER.getDebugOutput(out)
    }

    @JvmStatic
    fun id(path: String): ResourceIdentifier = ResourceIdentifier(MOD_ID, path)
}