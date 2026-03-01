package net.typho.vibrancy

import com.mojang.serialization.Lifecycle
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.GlFlag
import net.typho.big_shot_lib.api.client.opengl.util.OpenGL
import net.typho.big_shot_lib.api.client.opengl.util.TextureFormat
import net.typho.big_shot_lib.api.client.util.*
import net.typho.big_shot_lib.api.client.util.dynamic_buffers.AlbedoDynamicBuffer
import net.typho.big_shot_lib.api.client.util.dynamic_buffers.NormalsDynamicBuffer
import net.typho.big_shot_lib.api.client.util.events.ClientEventFactory
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.*
import net.typho.big_shot_lib.api.util.events.CommonEventFactory
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.block.BlockLightInfoLoader
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightRegistry
import net.typho.vibrancy.sky.SkyLightStorage
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.NativeResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Vibrancy {
    const val MOD_ID = "vibrancy"
    const val MOD_NAME = "Vibrancy"
    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

    val config: VibrancyConfig
        get() = AutoConfig.getConfigHolder(VibrancyConfig::class.java).config
    @JvmField
    val lightManager = LightManager()
    val outputFbo by lazy {
        NeoFramebuffer(
            listOf(NeoTexture2D(TextureFormat.RGB16F)),
            NeoTexture2D(TextureFormat.DEPTH24_STENCIL8),
            GlFramebuffer.MAIN.width.coerceAtLeast(1),
            GlFramebuffer.MAIN.height.coerceAtLeast(1)
        )
    }
    val worldPosFbo by lazy {
        NeoFramebuffer(
            listOf(NeoTexture2D(TextureFormat.RGB32F)),
            null,
            GlFramebuffer.MAIN.width.coerceAtLeast(1),
            GlFramebuffer.MAIN.height.coerceAtLeast(1)
        )
    }
    var reloadShadowsKey: KeyMapping? = null
    var toggleRaytracedLightsKey: KeyMapping? = null
    var toggleSubtleLightsKey: KeyMapping? = null

    init {
        val holder = AutoConfig.register(
            VibrancyConfig::class.java,
            ::GsonConfigSerializer
        )
        holder.registerLoadListener { holder, config ->
            OpenGL.INSTANCE.recordRenderCall {
                lightManager.reload()
            }

            return@registerLoadListener null
        }
        holder.registerSaveListener { holder, config ->
            OpenGL.INSTANCE.recordRenderCall {
                lightManager.reload()
            }

            return@registerSaveListener null
        }
    }

    @JvmStatic
    fun render(data: RenderEventData) {
        GlFlag.DEPTH_TEST.stack.push(false)

        worldPosFbo.bind()
        worldPosFbo.viewport()

        lightManager.blitWorldPos(data)

        worldPosFbo.unbind()

        outputFbo.bind()
        outputFbo.viewport()
        outputFbo.clear(ClearBit.Color(IColor.FULL_OFF))

        lightManager.render(data, outputFbo)

        outputFbo.unbind()

        GlFramebuffer.MAIN.bind(false)
        GlFramebuffer.MAIN.viewport() // TODO

        lightManager.blitOutput(data, outputFbo.colorAttachments[0] as GlTexture)

        GlFlag.DEPTH_TEST.stack.pop()
    }

    @JvmStatic
    fun id(path: String): ResourceIdentifier = ResourceIdentifier(MOD_ID, path)

    class Entrypoint : BigShotCommonEntrypoint, BigShotClientEntrypoint {
        override fun registerRegistries(factory: RegistryFactory) {
            BlockLightRegistry.registry = factory.create(
                BlockLightRegistry.registryKey.location,
                Lifecycle.stable(),
                false
            )
        }

        override fun registerContent(factory: RegistrationFactory) {
            BlockLightRegistry.registerBuiltins(factory)
        }

        override fun registerEvents(factory: CommonEventFactory) {
            factory.onBlockChanged { level, pos, old, new ->
                if (level.isClientSide()) {
                    OpenGL.INSTANCE.recordRenderCall {
                        lightManager.blockChanged(level, pos, old, new)
                    }
                }
            }
            factory.onChunkChanged { level, old, new ->
                if (level?.isClientSide() == true) {
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
            factory.register(id("block_lights"), BlockLightInfoLoader)
        }

        override fun registerKeyMappings(factory: KeyMappingFactory) {
            val category = factory.getOrCreateCategory(id("keys"))
            reloadShadowsKey = factory.create(id("rebuild_all_shadows"), GLFW.GLFW_KEY_F6, category)
            toggleRaytracedLightsKey = factory.create(id("toggle_raytraced_block_lights"), GLFW.GLFW_KEY_F7, category)
            toggleSubtleLightsKey = factory.create(id("toggle_subtle_block_lights"), GLFW.GLFW_KEY_F8, category)
        }

        override fun registerEvents(factory: ClientEventFactory) {
            factory.onLevelRenderEnd(Vibrancy::render)
            factory.onWindowResized { width, height ->
                outputFbo.resize(width, height)
                worldPosFbo.resize(width, height)
            }
            factory.onLevelChanged { old, new ->
                lightManager.clear()

                if (new == null) {
                    (lightManager.skyLight?.second as? NativeResource)?.free()
                    lightManager.skyLight = null
                } else {
                    BlockLightInfoLoader.onResourceManagerReload(WrapperUtil.INSTANCE.wrap(Minecraft.getInstance().resourceManager))

                    SkyLightRegistry.get(new)?.let { info ->
                        if (lightManager.skyLight?.first != info.type) {
                            (lightManager.skyLight?.second as? NativeResource)?.free()
                            lightManager.skyLight = null
                        }

                        if (lightManager.skyLight == null) {
                            lightManager.skyLight = info.type to info.type.createStorage(lightManager)
                        }

                        @Suppress("UNCHECKED_CAST")
                        fun <I : SkyLightInfo> load(storage: SkyLightStorage<I>) {
                            storage.load(lightManager, info as I)
                        }

                        load(lightManager.skyLight!!.second)
                    }
                }
            }
            factory.onFrameStart {
                fun debugPrint(text: Component) {
                    Minecraft.getInstance().gui.chat.addMessage(
                        Component.empty()
                            .append(Component.translatable("debug.prefix").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                            .append(CommonComponents.SPACE)
                            .append(text)
                    )
                }

                while (reloadShadowsKey?.consumeClick() == true) {
                    lightManager.reload()
                    debugPrint(
                        Component.translatable(
                            "debug.vibrancy.rebuild_all_shadows",
                            reloadShadowsKey!!.translatedKeyMessage
                        )
                    )
                }

                while (toggleRaytracedLightsKey?.consumeClick() == true) {
                    val enabled = !config.blockLights.raytraced.enabled
                    config.blockLights.raytraced.enabled = enabled
                    AutoConfig.getConfigHolder(VibrancyConfig::class.java).save()
                    debugPrint(
                        Component.translatable(
                            "debug.vibrancy.${if (enabled) "enable" else "disable"}_raytraced_block_lights",
                            toggleRaytracedLightsKey!!.translatedKeyMessage
                        )
                    )
                }

                while (toggleSubtleLightsKey?.consumeClick() == true) {
                    val enabled = !config.blockLights.subtle.enabled
                    config.blockLights.subtle.enabled = enabled
                    AutoConfig.getConfigHolder(VibrancyConfig::class.java).save()
                    debugPrint(
                        Component.translatable(
                            "debug.vibrancy.${if (enabled) "enable" else "disable"}_subtle_block_lights",
                            toggleSubtleLightsKey!!.translatedKeyMessage
                        )
                    )
                }
            }
        }

        override fun registerDebugScreenInfo(factory: DebugScreenFactory) {
            factory.register(id("debug_info"), false) { out ->
                out.accept(ChatFormatting.UNDERLINE.toString() + MOD_NAME)
                lightManager.getDebugOutput(out)
            }
        }

        override fun registerDynamicBuffers(factory: DynamicBufferFactory) {
            factory.register(NormalsDynamicBuffer)
            factory.register(AlbedoDynamicBuffer)
        }
    }
}