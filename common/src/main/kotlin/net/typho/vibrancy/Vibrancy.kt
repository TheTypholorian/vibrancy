package net.typho.vibrancy

import com.mojang.serialization.Lifecycle
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.client.opengl.util.OpenGL
import net.typho.big_shot_lib.api.client.util.*
import net.typho.big_shot_lib.api.client.util.events.ClientEventFactory
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.client.util.panoramas.PanoramaPriority
import net.typho.big_shot_lib.api.client.util.panoramas.PanoramaSet
import net.typho.big_shot_lib.api.client.util.panoramas.PanoramaTexture
import net.typho.big_shot_lib.api.util.BigShotCommonEntrypoint
import net.typho.big_shot_lib.api.util.RegistrationFactory
import net.typho.big_shot_lib.api.util.RegistryFactory
import net.typho.big_shot_lib.api.util.WrapperUtil
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
    @JvmField
    var reloadShadowsKey: KeyMapping? = null
    @JvmField
    var toggleRaytracedLightsKey: KeyMapping? = null
    @JvmField
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
        lightManager.render(data, data.target) // WrapperUtil.INSTANCE.wrap()
    }

    @JvmStatic
    fun id(path: String): ResourceIdentifier = ResourceIdentifier(MOD_ID, path)

    @JvmStatic
    fun AABB.toBlockBox() = BlockBox(
        BlockPos.containing(minPosition),
        BlockPos.containing(maxPosition)
    )

    @JvmStatic
    fun Direction.isPointingTowards(from: BlockPos, to: BlockPos): Boolean = when (this) {
        Direction.DOWN -> to.y < from.y
        Direction.UP -> to.y > from.y
        Direction.NORTH -> to.z < from.z
        Direction.SOUTH -> to.z > from.z
        Direction.WEST -> to.x < from.x
        Direction.EAST -> to.x > from.x
    }

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

        override fun registerPanoramas(factory: PanoramaFactory) {
            factory.register(PanoramaSet(
                id("panoramas"),
                PanoramaPriority.SHADER_PACK,
                listOf(
                    PanoramaTexture(id("textures/gui/title/background/ancient_city")),
                    PanoramaTexture(id("textures/gui/title/background/trial_chamber")),
                    PanoramaTexture(id("textures/gui/title/background/lush_cave"))
                )
            ))
        }
    }
}