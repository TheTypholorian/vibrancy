package net.typho.vibrancy

import com.mojang.serialization.Lifecycle
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.typho.big_shot_lib.api.client.registration.BigShotClientRegistrationEntrypoint
import net.typho.big_shot_lib.api.client.registration.DebugScreenFactory
import net.typho.big_shot_lib.api.client.registration.KeyMappingFactory
import net.typho.big_shot_lib.api.client.registration.ResourceListenerFactory
import net.typho.big_shot_lib.api.client.registration.events.ClientEventFactory
import net.typho.big_shot_lib.api.client.registration.events.RenderEventData
import net.typho.big_shot_lib.api.client.rendering.state.OpenGL
import net.typho.big_shot_lib.api.client.rendering.textures.*
import net.typho.big_shot_lib.api.registration.BigShotCommonRegistrationEntrypoint
import net.typho.big_shot_lib.api.registration.RegistrationFactory
import net.typho.big_shot_lib.api.registration.RegistryFactory
import net.typho.big_shot_lib.api.registration.events.CommonEventFactory
import net.typho.big_shot_lib.api.services.WrapperUtil
import net.typho.big_shot_lib.api.util.IColor
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.block.BlockLightInfoLoader
import net.typho.vibrancy.block.BlockLightRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Vibrancy : BigShotCommonRegistrationEntrypoint, BigShotClientRegistrationEntrypoint {
    const val MOD_ID = "vibrancy"
    const val MOD_NAME = "Vibrancy"
    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

    @JvmField
    val config = ConfigApi.registerAndLoadConfig(::VibrancyConfig, RegisterType.CLIENT)
    @JvmField
    val lightManager = LightManager()
    val outputFbo by lazy {
        NeoFramebuffer(
            listOf(NeoTexture2D(TextureFormat.RGB16F)),
            null,
            GlFramebuffer.MAIN.width(),
            GlFramebuffer.MAIN.height()
        )
    }
    val worldPosFbo by lazy {
        NeoFramebuffer(
            listOf(NeoTexture2D(TextureFormat.RGB32F)),
            null,
            GlFramebuffer.MAIN.width(),
            GlFramebuffer.MAIN.height()
        )
    }

    @JvmStatic
    fun render(data: RenderEventData) {
        worldPosFbo.bind()
        worldPosFbo.viewport()

        lightManager.blitWorldPos(data)

        worldPosFbo.unbind()

        outputFbo.bind()

        outputFbo.viewport()
        outputFbo.clear(ClearBit.Color(IColor.BLACK))

        lightManager.render(data, outputFbo)

        outputFbo.unbind()

        GlFramebuffer.MAIN.bind(false)
        GlFramebuffer.MAIN.viewport() // TODO

        lightManager.blitOutput(data, outputFbo.colorAttachments[0] as GlTexture)
    }

    @JvmStatic
    fun id(path: String): ResourceIdentifier = ResourceIdentifier(MOD_ID, path)

    override fun registerRegistries(factory: RegistryFactory) {
        BlockLightRegistry.registry = WrapperUtil.INSTANCE.wrap(
            factory.create(
                BlockLightRegistry.registryKey.location,
                Lifecycle.stable()
            )
        )
    }

    override fun registerContent(factory: RegistrationFactory) {
        BlockLightRegistry.registerBuiltins(factory)
    }

    override fun registerEvents(factory: CommonEventFactory) {
        factory.onBlockChanged { level, pos, old, new ->
            if (level.isClientSide) {
                OpenGL.INSTANCE.recordRenderCall {
                    lightManager.blockChanged(level, pos, old, new)
                }
            }
        }
        factory.onChunkChanged { old, new ->
            if (old?.level?.isClientSide == true || new?.level?.isClientSide == true) {
                OpenGL.INSTANCE.recordRenderCall {
                    if (old != null) {
                        lightManager.deloadChunk(old)
                    }

                    if (new != null) {
                        lightManager.loadChunk(new)
                    }
                }
            }
        }
    }

    override fun registerReloadListeners(factory: ResourceListenerFactory) {
    }

    override fun registerKeyMappings(factory: KeyMappingFactory) {
    }

    override fun registerEvents(factory: ClientEventFactory) {
        factory.onLevelRenderEnd(this::render)
        factory.onWindowResized { width, height ->
            outputFbo.resize(width, height)
            worldPosFbo.resize(width, height)
        }
        factory.onLevelChanged { old, new ->
            lightManager.clear()
            BlockLightInfoLoader.load(WrapperUtil.INSTANCE.wrap(Minecraft.getInstance().resourceManager))
        }
    }

    override fun registerDebugScreenInfo(factory: DebugScreenFactory) {
        factory.register(id("debug_info"), false) { out ->
            out.accept(ChatFormatting.UNDERLINE.toString() + MOD_NAME)
            lightManager.getDebugOutput(out)
        }
    }
}