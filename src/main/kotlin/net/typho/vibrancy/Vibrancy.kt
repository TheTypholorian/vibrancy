package net.typho.vibrancy

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.typho.big_shot_lib.api.NeoCommonInitializer
import net.typho.big_shot_lib.api.client.NeoClientInitializer
import net.typho.big_shot_lib.api.client.event.AddAssetReloadListenersEvent
import net.typho.big_shot_lib.api.client.event.ClientEndFrameEvent
import net.typho.big_shot_lib.api.client.event.ClientLevelChangedEvent
import net.typho.big_shot_lib.api.client.event.ClientStartFrameEvent
import net.typho.big_shot_lib.api.client.rendering.common.GpuDrawSettings
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.GpuQueue
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuAlphaFunction
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBlendFunction
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuDataType
import net.typho.big_shot_lib.api.event.BlockChangedEvent
import net.typho.big_shot_lib.api.event.ChunkLoadedEvent
import net.typho.big_shot_lib.api.event.ChunkUnloadedEvent
import net.typho.big_shot_lib.api.event.NeoClientEventBus
import net.typho.big_shot_lib.api.event.NeoEventBus
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.block.BlockLightInfoLoader
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.util.ExtraAtlases
import net.typho.vibrancy.sky.SkyLightInfoLoader
import net.typho.vibrancy.sky.SkyLightRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Vibrancy : NeoCommonInitializer, NeoClientInitializer {
    override val modId: String = "vibrancy"
    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger("Vibrancy")

    @JvmField
    val lightManager = LightManager()
    @JvmField
    var disableFlywheelInstancing = false

    @JvmField
    val entityShadowTextureBlacklist = hashSetOf(
        Identifier.minecraft("textures/entity/beacon_beam.png"),
        Identifier.minecraft("textures/misc/enchanted_glint_entity.png"),
        Identifier.minecraft("textures/misc/enchanted_glint_item.png"),
        Identifier.minecraft("textures/misc/enchanted_item_glint.png"),
    )

    @JvmField
    val raytracedPointRenderType = GpuObjects.renderType(
        id("raytraced_point"),
        CompactChunkVertex.VERTEX_FORMAT,
        GpuDrawSettings.Builder()
            .blend(GpuBlendFunction.ADDITIVE)
            .shader(id("raytraced_point"))
            .cull()
            .depth(GpuAlphaFunction.gequal)
            .writeDepth(false)
            .zOffset()
            .sampler("u_BlockTex")
            .sampler("u_MaterialTex")
            .sampler("u_TransmissionTex")
            .uniform("Globals")
            .uniform("u_Globals")
            .uniform("u_VibrancyConfig")
            .storageBuffer("u_Lights")
            .storageBuffer("u_Shadows")
            .storageBuffer("u_Grids")
            .texelBuffer("u_SectionTimeInfo", GpuDataType.sint32, 1),
        RenderType.SMALL_BUFFER_SIZE,
        false,
        true,
        false
    )

    @JvmField
    val entityShadowRenderType = GpuObjects.renderType(
        id("entity_shadow"),
        DefaultVertexFormat.POSITION_TEX,
        GpuDrawSettings.Builder()
            .blend(GpuBlendFunction.TRANSLUCENT) // TODO
            .shader(id("entity_shadow"))
            .cull()
            .depth(GpuAlphaFunction.gequal)
            .writeDepth(false)
            .zOffset()
            .sampler("u_BaseTex")
            //.sampler("u_MaterialTex")
            .sampler("u_TransmissionTex")
            .uniform("DynamicTransforms")
            .uniform("Projection")
            .uniform("u_VibrancyConfig"),
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        false
    )
    val blitVertexBuffer by lazy {
        GpuObjects.buffer(
            { "Vibrancy Blit Vertex Buffer" },
            20L * 6,
            GpuBufferUsage.VERTEX
        ) { output ->
            fun vertex(x: Float, y: Float) {
                output.writeFloat(x * 2 - 1)
                output.writeFloat(y * 2 - 1)
                output.writeFloat(0f)
                output.writeFloat(x)
                output.writeFloat(y)
            }

            vertex(0f, 0f)
            vertex(1f, 0f)
            vertex(1f, 1f)

            vertex(1f, 1f)
            vertex(0f, 1f)
            vertex(0f, 0f)
        }
    }
    @JvmField
    val entityShadowBlitRenderType = GpuObjects.renderType(
        id("entity_shadow_blit"),
        DefaultVertexFormat.POSITION_TEX,
        GpuDrawSettings.Builder()
            .blend(GpuBlendFunction.TRANSLUCENT)
            .shader(id("entity_shadow_blit"))
            .depth(GpuAlphaFunction.always)
            .writeDepth(false)
            .sampler("u_ShadowTex")
            .uniform("u_VibrancyConfig"),
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        false
    )

    /*
    @JvmField
    var reloadShadowsKey: KeyMapping? = null
    @JvmField
    var toggleRaytracedLightsKey: KeyMapping? = null
    @JvmField
    var toggleSubtleLightsKey: KeyMapping? = null
     */

    override fun addClientListener(listener: Consumer<NeoClientEventBus>) {
        super<NeoClientInitializer>.addClientListener(listener)
    }

    @Suppress("RedundantSamConstructor", "RedundantSuppression")
    override fun onInitialize(bus: NeoEventBus) {
        BlockLightRegistry.onInitialize(bus)
        SkyLightRegistry.onInitialize(bus)

        bus.register(BlockChangedEvent { level, pos, old, new ->
            if (VibrancyConfig.modEnabled && level.isClientSide) {
                GpuQueue.runOrQueue {
                    lightManager.blockChanged(level, pos, old, new)
                }
            }
        })
        bus.register(ChunkLoadedEvent { level, chunk ->
            GpuQueue.runOrQueue {
                lightManager.loadChunk(chunk)
            }
        })
        bus.register(ChunkUnloadedEvent { level, chunk ->
            GpuQueue.runOrQueue {
                lightManager.deloadChunk(chunk)
            }
        })
    }

    @Suppress("RedundantSamConstructor", "RedundantSuppression")
    override fun onInitializeClient(bus: NeoClientEventBus) {
        // TODO
        ResourceLoader.registerBuiltinPack(id("bare_bones_compat"), FabricLoader.getInstance().getModContainer(modId).orElseThrow(), PackActivationType.NORMAL)

        bus.register(AddAssetReloadListenersEvent { output ->
            output(BlockLightInfoLoader)
            output(SkyLightInfoLoader)
            output(ExtraAtlases)
        })
        bus.register(ClientLevelChangedEvent { old, new ->
            if (VibrancyConfig.modEnabled) {
                lightManager.levelChanged(old, new)
            }
        })
        bus.register(ClientStartFrameEvent {
            lightManager.preRender()
        })
        bus.register(ClientEndFrameEvent {
            lightManager.postRender()
        })
        ExtraAtlases.onInitializeClient(bus)
        /*
        bus.register(RegisterDebugScreenEntriesEvent { output ->
            output(DebugScreenEntry(id("debug_info"), false) { out ->
                out.accept(ChatFormatting.UNDERLINE.toString() + "Vibrancy")
                lightManager.getDebugOutput(out)
            })
        })
         */
    }

    /*
    @JvmStatic
    fun render(data: RenderEventData) {
        if (VibrancyConfig.modEnabled) {
            //? if <1.21 {
            /*tickDelta = Minecraft.getInstance().frameTime
            *///? } else if <1.21.2 {
            /*tickDelta = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(false)
            *///? } else {
            tickDelta = Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(false)
            //? }

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

                TEMP_FRAMEBUFFER.bind(IRect2(0, 0, width, height)).use { fbo ->
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
     */

    @JvmStatic
    fun id(path: String): Identifier = Identifier.of(modId, path)

    @JvmStatic
    fun Direction.isPointingTowards(from: IVec3<Int>, to: IVec3<Int>): Boolean = when (this) {
        Direction.DOWN -> to.y < from.y
        Direction.UP -> to.y > from.y
        Direction.NORTH -> to.z < from.z
        Direction.SOUTH -> to.z > from.z
        Direction.WEST -> to.x < from.x
        Direction.EAST -> to.x > from.x
    }

    @JvmStatic
    fun Direction.isPointingTowardsInclusive(from: IVec3<Int>, to: IVec3<Int>): Boolean = when (this) {
        Direction.DOWN -> to.y <= from.y
        Direction.UP -> to.y >= from.y
        Direction.NORTH -> to.z <= from.z
        Direction.SOUTH -> to.z >= from.z
        Direction.WEST -> to.x <= from.x
        Direction.EAST -> to.x >= from.x
    }

    /*
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
     */

    /*
    override fun registerEvents(factory: CommonEventFactory) {
        factory.blockChanged.add { level, pos, old, new ->
            if (VibrancyConfig.modEnabled && level.isClientSide()) {
                GlQueue.INSTANCE.runOrQueue {
                    lightManager.blockChanged(level, pos, old, new)
                }
            }
        }
    }
     */

    /*
    override fun registerReloadListeners(factory: ResourceListenerFactory) {
        factory.register(BlockLightInfoLoader)
        factory.register(SkyLightInfoLoader)
    }
     */

    /*
    override fun registerKeyMappings(factory: KeyMappingFactory) {
        val category = factory.getOrCreateCategory(id("keys"))
        reloadShadowsKey = factory.create(id("rebuild_all_shadows"), GLFW.GLFW_KEY_F6, category)
        toggleRaytracedLightsKey = factory.create(id("toggle_raytraced_block_lights"), GLFW.GLFW_KEY_F7, category)
        toggleSubtleLightsKey = factory.create(id("toggle_subtle_block_lights"), GLFW.GLFW_KEY_F8, category)
    }
     */

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