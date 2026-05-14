package net.typho.vibrancy

import com.mojang.serialization.Lifecycle
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.typho.big_shot_lib.api.client.rendering.opengl.GlQueue
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlResourceType
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.TextureMipmapLevel
import net.typho.big_shot_lib.api.client.rendering.opengl.state.*
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.opengl.util.ColorMask
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.client.util.BigShotClientEntrypoint
import net.typho.big_shot_lib.api.client.util.DebugScreenFactory
import net.typho.big_shot_lib.api.client.util.InitialScreenFactory
import net.typho.big_shot_lib.api.client.util.ResourceListenerFactory
import net.typho.big_shot_lib.api.client.util.event.ClientEventFactory
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.*
import net.typho.big_shot_lib.api.util.event.CommonEventFactory
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.block.BlockLightInfoLoader
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.impl.SubtleLightStorage
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.ShadowBuffer
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightInfoLoader
import net.typho.vibrancy.sky.SkyLightRegistry
import net.typho.vibrancy.sky.SkyLightStorage
import net.typho.vibrancy.sky.impl.OverworldSkyLightStorage
import org.lwjgl.opengl.GL
import org.lwjgl.system.NativeResource
import org.lwjgl.system.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Vibrancy : BigShotCommonEntrypoint, BigShotClientEntrypoint {
    const val MOD_ID = "vibrancy"
    const val MOD_NAME = "Vibrancy"
    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

    @JvmField
    val lightManager = LightManager()
    @JvmField
    var disableFlywheelInstancing = false
    @JvmField
    var tickDelta = 0f

    /*
    @JvmField
    var reloadShadowsKey: KeyMapping? = null
    @JvmField
    var toggleRaytracedLightsKey: KeyMapping? = null
    @JvmField
    var toggleSubtleLightsKey: KeyMapping? = null
     */

    val RESULT by lazy {
        NeoGlTexture2D().also {
            it.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                texture.textureDataMutable(1, 1, GlTextureFormat.RGB16F)
                texture.minFilter = GlTextureMinFilter.NEAREST
                texture.magFilter = GlTextureMagFilter.NEAREST
            }
        }
    }
    val TEMP by lazy {
        NeoGlTexture2D().also {
            it.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                texture.textureDataMutable(1, 1, GlTextureFormat.RGB16F)
                texture.minFilter = GlTextureMinFilter.NEAREST
                texture.magFilter = GlTextureMagFilter.NEAREST
            }
        }
    }
    val DEPTH by lazy {
        NeoGlTexture2D().also {
            it.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                texture.textureDataMutable(1, 1, GlTextureFormat.DEPTH_COMPONENT)
                texture.minFilter = GlTextureMinFilter.NEAREST
                texture.magFilter = GlTextureMagFilter.NEAREST
            }
        }
    }
    val RESULT_FRAMEBUFFER by lazy {
        NeoGlFramebuffer().also {
            it.bind(null).use { fbo ->
                fbo.colorAttachments[0] = RESULT
                fbo.depthAttachment = DEPTH
                fbo.checkStatus().throwIfError()
            }
        }
    }
    val TEMP_FRAMEBUFFER by lazy {
        NeoGlFramebuffer().also {
            it.bind(null).use { fbo ->
                fbo.colorAttachments[0] = TEMP
                fbo.depthAttachment = DEPTH
                fbo.checkStatus().throwIfError()
            }
        }
    }

    @JvmStatic
    fun depthBlitState(from: GlTexture2D) = GlDrawState.Basic(
        colorMask = GlColorMaskShard(ColorMask(false, false, false, false)),
        depth = GlDepthShard.Enabled(
            GlAlphaFunction.ALWAYS,
            true
        ),
        shader = GlShaderShard.FromLocation(
            id("depth_blit"),
            { },
            GlTextureBinding.FromInstance(from, GlTextureTarget.TEXTURE_2D)
        )
    )

    @JvmStatic
    fun render(data: RenderEventData) {
        if (VibrancyConfig.modEnabled) {
            //? if <1.21 {
            /*tickDelta = Minecraft.getInstance().frameTime
            *///? } else if <1.21.2 {
            tickDelta = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(false)
            //? } else {
            /*tickDelta = Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(false)
            *///? }

            val targetAttachment = data.target.colorAttachments[0]!!
            val width = targetAttachment.width!!.coerceAtLeast(1)
            val height = targetAttachment.height!!.coerceAtLeast(1)

            TEMP_FRAMEBUFFER.bind().use { fbo ->
                if (TEMP.width != width || TEMP.height != height) {
                    RESULT.bind(GlTextureTarget.TEXTURE_2D).use {
                        it.textureDataMutable(width, height, GlTextureFormat.RGB16F)
                    }
                    TEMP.bind(GlTextureTarget.TEXTURE_2D).use {
                        it.textureDataMutable(width, height, GlTextureFormat.RGB16F)
                    }
                    DEPTH.bind(GlTextureTarget.TEXTURE_2D).use {
                        it.textureDataMutable(
                            width,
                            height,
                            GlTextureFormat.DEPTH_COMPONENT
                        )
                    }
                }

                TEMP_FRAMEBUFFER.bind(NeoRect2i(0, 0, width, height)).use { fbo ->
                    fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF), GlClearBit.Depth(1f))

                    depthBlitState(data.target.depthAttachment as GlTexture2D).bind().use {
                        Mesh.SCREEN_MESH.draw()
                    }

                    RESULT_FRAMEBUFFER.bind().use { fbo ->
                        fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))
                    }

                    lightManager.render(
                        data,
                        RESULT_FRAMEBUFFER,
                        TEMP_FRAMEBUFFER
                    )
                }
            }

            lightManager.blitFromTemp(data.target, RESULT_FRAMEBUFFER, lightLimited = false)
        }
    }

    @JvmStatic
    fun id(path: String): NeoIdentifier = NeoIdentifier(MOD_ID, path)

    @JvmStatic
    fun NeoDirection.isPointingTowards(from: IVec3<Int>, to: IVec3<Int>): Boolean = when (this) {
        NeoDirection.DOWN -> to.y < from.y
        NeoDirection.UP -> to.y > from.y
        NeoDirection.NORTH -> to.z < from.z
        NeoDirection.SOUTH -> to.z > from.z
        NeoDirection.WEST -> to.x < from.x
        NeoDirection.EAST -> to.x > from.x
    }

    @JvmStatic
    fun NeoDirection.isPointingTowardsInclusive(from: IVec3<Int>, to: IVec3<Int>): Boolean = when (this) {
        NeoDirection.DOWN -> to.y <= from.y
        NeoDirection.UP -> to.y >= from.y
        NeoDirection.NORTH -> to.z <= from.z
        NeoDirection.SOUTH -> to.z >= from.z
        NeoDirection.WEST -> to.x <= from.x
        NeoDirection.EAST -> to.x >= from.x
    }

    @JvmStatic
    fun NeoDirection.isPointingTowards(from: BlockPos, to: IVec3<Int>): Boolean = when (this) {
        NeoDirection.DOWN -> to.y < from.y
        NeoDirection.UP -> to.y > from.y
        NeoDirection.NORTH -> to.z < from.z
        NeoDirection.SOUTH -> to.z > from.z
        NeoDirection.WEST -> to.x < from.x
        NeoDirection.EAST -> to.x > from.x
    }

    @JvmStatic
    fun NeoDirection.isPointingTowardsInclusive(from: BlockPos, to: IVec3<Int>): Boolean = when (this) {
        NeoDirection.DOWN -> to.y <= from.y
        NeoDirection.UP -> to.y >= from.y
        NeoDirection.NORTH -> to.z <= from.z
        NeoDirection.SOUTH -> to.z >= from.z
        NeoDirection.WEST -> to.x <= from.x
        NeoDirection.EAST -> to.x >= from.x
    }

    @JvmStatic
    fun NeoDirection.isPointingTowards(from: BlockPos, to: BlockPos): Boolean = when (this) {
        NeoDirection.DOWN -> to.y < from.y
        NeoDirection.UP -> to.y > from.y
        NeoDirection.NORTH -> to.z < from.z
        NeoDirection.SOUTH -> to.z > from.z
        NeoDirection.WEST -> to.x < from.x
        NeoDirection.EAST -> to.x > from.x
    }

    @JvmStatic
    fun NeoDirection.isPointingTowardsInclusive(from: BlockPos, to: BlockPos): Boolean = when (this) {
        NeoDirection.DOWN -> to.y <= from.y
        NeoDirection.UP -> to.y >= from.y
        NeoDirection.NORTH -> to.z <= from.z
        NeoDirection.SOUTH -> to.z >= from.z
        NeoDirection.WEST -> to.x <= from.x
        NeoDirection.EAST -> to.x >= from.x
    }

    override fun displayInitialScreens(factory: InitialScreenFactory) {
        if (VibrancyConfig.modEnabled) {
            if (!GL.getCapabilities().GL_ARB_shader_storage_buffer_object) {
                VibrancyConfig.modEnabled = false
                VibrancyConfig.save()
                factory.display(Component.translatable(if (Platform.get() == Platform.MACOSX) "error.vibrancy.no_ssbos_mac" else "error.vibrancy.no_ssbos"))
            }

            /*
            if (glGetString(GL_VENDOR)?.lowercase()?.contains("amd") == true) {
                VibrancyConfig.modEnabled = false
                VibrancyConfig.save()
                factory.display(Component.translatable("error.vibrancy.amd"))
            }
             */
        }
    }

    override fun registerRegistries(factory: RegistryFactory) {
        BlockLightRegistry.registry = factory.create(
            BlockLightRegistry.registryKey.location,
            Lifecycle.stable(),
            false
        )
        SkyLightRegistry.registry = factory.create(
            SkyLightRegistry.registryKey.location,
            Lifecycle.stable(),
            false
        )
    }

    override fun registerContent(factory: RegistrationFactory) {
        BlockLightRegistry.registerBuiltins(factory)
        SkyLightRegistry.registerBuiltins(factory)

        factory.begin(NeoVertexFormat.REGISTRY_KEY)?.run {
            register(id("shadow_mesh")) { ShadowBuffer.VERTEX_FORMAT }
            register(id("light_mesh")) { LightMesh.VERTEX_FORMAT }
            register(id("sky_mesh")) { LightMesh.SKY_VERTEX_FORMAT }
            register(id("subtle_mesh")) { SubtleLightStorage.VERTEX_FORMAT }
            register(id("light_mesh_inventory")) { LightMesh.INVENTORY_VERTEX_FORMAT }
            register(id("light_mesh_blit")) { LightMesh.BLIT_VERTEX_FORMAT }
        }
    }

    override fun registerEvents(factory: CommonEventFactory) {
        factory.blockChanged.add { level, pos, old, new ->
            if (VibrancyConfig.modEnabled && level.isClientSide()) {
                GlQueue.INSTANCE.runOrQueue {
                    lightManager.blockChanged(level, pos, old, new)
                }
            }
        }
    }

    override fun registerReloadListeners(factory: ResourceListenerFactory) {
        factory.register(BlockLightInfoLoader)
        factory.register(SkyLightInfoLoader)
    }

    /*
    override fun registerKeyMappings(factory: KeyMappingFactory) {
        val category = factory.getOrCreateCategory(id("keys"))
        reloadShadowsKey = factory.create(id("rebuild_all_shadows"), GLFW.GLFW_KEY_F6, category)
        toggleRaytracedLightsKey = factory.create(id("toggle_raytraced_block_lights"), GLFW.GLFW_KEY_F7, category)
        toggleSubtleLightsKey = factory.create(id("toggle_subtle_block_lights"), GLFW.GLFW_KEY_F8, category)
    }
     */

    override fun registerEvents(factory: ClientEventFactory) {
        factory.levelRenderEnd.add(Vibrancy::render)
        factory.levelChanged.add { old, new ->
            if (VibrancyConfig.modEnabled) {
                lightManager.clear()

                if (new == null) {
                    (lightManager.skyLight?.second as? NativeResource)?.free()
                    lightManager.skyLight = null
                } else {
                    val resourceManager = WrapperUtil.INSTANCE.wrap(Minecraft.getInstance().resourceManager)
                    BlockLightInfoLoader.onResourceManagerReload(resourceManager)
                    SkyLightInfoLoader.onResourceManagerReload(resourceManager)

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
        }
        /*
        factory.clientTickStart.add {
            fun debugPrint(text: Component) {
                Minecraft.getInstance().gui.chat.addMessage(
                    Component.empty()
                        .append(
                            Component.translatable("debug.prefix").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                        )
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
         */
        factory.chunkChanged.add { level, old, new ->
            if (VibrancyConfig.modEnabled && level.isClientSide()) {
                GlQueue.INSTANCE.runOrQueue {
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

    override fun registerDebugScreenInfo(factory: DebugScreenFactory) {
        factory.register(id("debug_info"), false) { out ->
            out(ChatFormatting.UNDERLINE.toString() + MOD_NAME)
            lightManager.getDebugOutput(out)
        }
    }

    /*
        override fun registerPanoramas(factory: PanoramaFactory) {
            factory.register(
                PanoramaSet(
                    id("panoramas"),
                    PanoramaPriority.SHADER_PACK,
                    listOf(
                        PanoramaTexture(id("textures/gui/title/background/ancient_city")),
                        PanoramaTexture(id("textures/gui/title/background/trial_chamber")),
                        PanoramaTexture(id("textures/gui/title/background/lush_cave"))
                    )
                )
            )
        }
         */
}