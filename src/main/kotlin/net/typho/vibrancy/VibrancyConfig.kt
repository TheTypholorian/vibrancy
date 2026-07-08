package net.typho.vibrancy

import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.GpuQueue
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.big_shot_lib.api.util.platform.PlatformUtil
import net.typho.vibrancy.block.impl.RayPointLightStorage
import net.typho.vibrancy.block.impl.RayPointLightType
import net.typho.vibrancy.block.impl.SubtleLightCullingMode
import net.typho.vibrancy.util.VibrancyThreadPool
import java.nio.file.Files
import kotlin.reflect.KMutableProperty0

internal fun <T : Any> Option.Builder<T>.binding(def: T, property: KMutableProperty0<T>): Option.Builder<T> {
    return binding(def, { property.get() }, { property.set(it) })
}

object VibrancyConfig {
    val configBuffer by lazy { GpuObjects.buffer({ "Vibrancy Config UBO" }, 64L, GpuBufferUsage.UNIFORM or GpuBufferUsage.COPY_DST) }
    @JvmField
    var configBufferDirty = true

    @JvmStatic
    fun loadConfigBuffer(): GpuBuffer {
        val buffer = configBuffer

        if (configBufferDirty) {
            configBufferDirty = false

            buffer.upload { output ->
                output.writeInt(if (limitLightBrightness) 1 else 0)
                output.writeInt(if (alignPixels) 1 else 0)
                output.writeInt(if (raycastLightModel) 1 else 0)
                output.writeFloat(rayLightBrightness)
                output.writeFloat(subtleLightBrightness)
                output.writeFloat(1f) // TODO
                output.writeFloat(skyLightBrightness)
                output.skip(4)

                output.writeInt(if (reflectionsEnabled) 1 else 0)
                output.writeFloat(reflectionStrength)
                output.writeFloat(reflectionExponent)
            }
        }

        return buffer
    }

    var modEnabled = true
        set(value) {
            field = value
            GpuQueue.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @JvmField
    var useMultithreading = true
    var asyncThreads: Int = 4
        set(value) {
            if (value < field) {
                VibrancyThreadPool.corePoolSize = value
                VibrancyThreadPool.maximumPoolSize = value
            } else {
                VibrancyThreadPool.maximumPoolSize = value
                VibrancyThreadPool.corePoolSize = value
            }

            field = value
        }
    var limitLightBrightness = false
        set(value) {
            field = value
            configBufferDirty = true
        }
    var alignPixels = true
        set(value) {
            field = value
            configBufferDirty = true
        }
    var raycastLightModel = false
        set(value) {
            field = value
            configBufferDirty = true
        }
    @JvmField
    var flickerStrength = 1f

    var reflectionsEnabled = true
        set(value) {
            field = value
            configBufferDirty = true
        }
    var reflectionStrength = 3.5f
        set(value) {
            field = value
            configBufferDirty = true
        }
    var reflectionExponent = 3f
        set(value) {
            field = value
            configBufferDirty = true
        }

    @JvmField
    var entityShadowsEnabled = true
    @JvmField
    var blockEntityShadows = true
    @JvmField
    var entityShadowDistance = 3
    @JvmField
    var entityShadowMaxBlockLights = 10

    var rayLightsEnabled = true
        set(value) {
            field = value
            GpuQueue.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @JvmField
    var rayLightsMaxRendered: Int = 400
    var rayLightBrightness: Float = 1f
        set(value) {
            field = value
            configBufferDirty = true
        }
    var rayLightShadowRadius: Int = 6
        set(value) {
            field = value

            Vibrancy.lightManager.blockLights[RayPointLightType]?.let {
                for (light in (it as RayPointLightStorage).map.values) {
                    light.shadowBox = light.createShadowBox()
                }
            }
        }
    @JvmField
    var rayLightMaxHighQuality: Int = 10

    var subtleLightsEnabled = true
        set(value) {
            field = value
            GpuQueue.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @JvmField
    var subtleLightsRenderDistance: Int = 6
    var subtleLightBrightness = 1f
        set(value) {
            field = value
            configBufferDirty = true
        }
    var subtleLightCullingMode = SubtleLightCullingMode.SOLID_NEIGHBOR
        set(value) {
            field = value
            GpuQueue.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }

    var skyLightsEnabled = true
        set(value) {
            field = value
            GpuQueue.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @JvmField
    var skyLightShadowDistance: Int = 16
    var skyLightBrightness: Float = 1f
        set(value) {
            field = value
            configBufferDirty = true
        }
    var skyLightResolution: Int = 2
        set(value) {
            field = value
            /*
            (Vibrancy.lightManager.skyLight?.second as? OverworldSkyLightStorage)?.let {
                val size = 1 shl (value + 10)
                it.texture.resize(size, size)
                it.translucent.resize(size, size)
            }
             */
        }
    @JvmField
    var skyLightShadowMapPower: Float = 8f
    @JvmField
    var skyLightTranslucentEnabled: Boolean = true

    @JvmStatic
    fun save() {
        JsonWriter(Files.newBufferedWriter(PlatformUtil.configPath.resolve("vibrancy.json"))).use { writer ->
            writer.setIndent("    ")
            writer.beginObject()

                .name("modEnabled").value(modEnabled)
                .name("useMultithreading").value(useMultithreading)
                .name("asyncThreads").value(asyncThreads)
                .name("limitLightBrightness").value(limitLightBrightness)
                .name("alignPixels").value(alignPixels)
                .name("raycastLightModel").value(raycastLightModel)
                .name("flickerStrength").value(flickerStrength)

                .name("specularReflections").beginObject()

                .name("enabled").value(reflectionsEnabled)
                .name("strength").value(reflectionStrength)
                .name("exponent").value(reflectionExponent)

                .endObject()

                .name("entityShadows").beginObject()

                .name("enabled").value(entityShadowsEnabled)
                .name("blockEntities").value(blockEntityShadows)
                .name("distance").value(entityShadowDistance)
                .name("maxLights").value(entityShadowMaxBlockLights)

                .endObject()

                .name("blockLights").beginObject()

                .name("raytraced").beginObject()

                .name("enabled").value(rayLightsEnabled)
                .name("maxRendered").value(rayLightsMaxRendered)
                .name("brightness").value(rayLightBrightness)
                .name("shadowRadius").value(rayLightShadowRadius)
                .name("maxHighQuality").value(rayLightMaxHighQuality)

                .endObject()

                .name("subtle").beginObject()

                .name("enabled").value(subtleLightsEnabled)
                .name("renderDistance").value(subtleLightsRenderDistance)
                .name("brightness").value(subtleLightBrightness)
                .name("cullingMode").value(subtleLightCullingMode.name)

                .endObject()

                .endObject()

                .name("skyLights").beginObject()

                .name("enabled").value(skyLightsEnabled)
                .name("shadowDistance").value(skyLightShadowDistance)
                .name("brightness").value(skyLightBrightness)
                .name("resolution").value(skyLightResolution)
                .name("shadowMapPower").value(skyLightShadowMapPower)
                .name("translucentEnabled").value(skyLightTranslucentEnabled)

                .endObject()

                .endObject()
        }
    }

    @JvmStatic
    fun load() {
        val path = PlatformUtil.configPath.resolve("vibrancy.json")

        if (Files.exists(path)) {
            try {
                val json = Files.newBufferedReader(path).use { JsonParser.parseReader(it) }.asJsonObject

                json.getAsJsonPrimitive("modEnabled")?.let { modEnabled = it.asBoolean }
                json.getAsJsonPrimitive("useMultithreading")?.let { useMultithreading = it.asBoolean }
                json.getAsJsonPrimitive("asyncThreads")?.let { asyncThreads = it.asInt }
                json.getAsJsonPrimitive("limitLightBrightness")?.let { limitLightBrightness = it.asBoolean }
                json.getAsJsonPrimitive("alignPixels")?.let { alignPixels = it.asBoolean }
                json.getAsJsonPrimitive("raycastLightModel")?.let { raycastLightModel = it.asBoolean }
                json.getAsJsonPrimitive("flickerStrength")?.let { flickerStrength = it.asFloat }

                json.getAsJsonObject("specularReflections")?.let { reflections ->
                    reflections.getAsJsonPrimitive("enabled")?.let { reflectionsEnabled = it.asBoolean }
                    reflections.getAsJsonPrimitive("strength")?.let { reflectionStrength = it.asFloat }
                    reflections.getAsJsonPrimitive("exponent")?.let { reflectionExponent = it.asFloat }
                }

                json.getAsJsonObject("entityShadows")?.let { entityShadows ->
                    entityShadows.getAsJsonPrimitive("enabled")?.let { entityShadowsEnabled = it.asBoolean }
                    entityShadows.getAsJsonPrimitive("blockEntities")?.let { blockEntityShadows = it.asBoolean }
                    entityShadows.getAsJsonPrimitive("distance")?.let { entityShadowDistance = it.asInt }
                    entityShadows.getAsJsonPrimitive("maxLights")?.let { entityShadowMaxBlockLights = it.asInt }
                }

                json.getAsJsonObject("blockLights")?.let { blockLights ->
                    blockLights.getAsJsonObject("raytraced")?.let { raytraced ->
                        raytraced.getAsJsonPrimitive("enabled")?.let { rayLightsEnabled = it.asBoolean }
                        raytraced.getAsJsonPrimitive("maxRendered")?.let { rayLightsMaxRendered = it.asInt }
                        raytraced.getAsJsonPrimitive("brightness")?.let { rayLightBrightness = it.asFloat }
                        raytraced.getAsJsonPrimitive("shadowRadius")?.let { rayLightShadowRadius = it.asInt }
                        raytraced.getAsJsonPrimitive("maxHighQuality")?.let { rayLightMaxHighQuality = it.asInt }
                    }

                    blockLights.getAsJsonObject("subtle")?.let { subtle ->
                        subtle.getAsJsonPrimitive("enabled")?.let { subtleLightsEnabled = it.asBoolean }
                        subtle.getAsJsonPrimitive("renderDistance")?.let { subtleLightsRenderDistance = it.asInt }
                        subtle.getAsJsonPrimitive("brightness")?.let { subtleLightBrightness = it.asFloat }
                        subtle.getAsJsonPrimitive("cullingMode")?.let { subtleLightCullingMode = SubtleLightCullingMode.valueOf(it.asString.uppercase()) }
                    }
                }

                json.getAsJsonObject("skyLights")?.let { skyLights ->
                    skyLights.getAsJsonPrimitive("enabled")?.let { skyLightsEnabled = it.asBoolean }
                    skyLights.getAsJsonPrimitive("shadowDistance")?.let { skyLightShadowDistance = it.asInt }
                    skyLights.getAsJsonPrimitive("brightness")?.let { skyLightBrightness = it.asFloat }
                    skyLights.getAsJsonPrimitive("resolution")?.let { skyLightResolution = it.asInt }
                    skyLights.getAsJsonPrimitive("shadowMapPower")?.let { skyLightShadowMapPower = it.asFloat }
                    skyLights.getAsJsonPrimitive("translucentEnabled")?.let { skyLightTranslucentEnabled = it.asBoolean }
                }
            } catch (e: Exception) {
                Vibrancy.LOGGER.info("Error loading Vibrancy config", e)
                save()
            }
        } else {
            Files.createFile(path)
            save()
        }
    }

    init {
        load()
    }

    @JvmStatic
    fun createScreen(parent: Screen?): Screen {
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("config.vibrancy.title"))
            .save { save() }

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.general"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.general.modEnabled"))
                    .binding(true, VibrancyConfig::modEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .also {
                    if (PlatformUtil.isDevEnv()) {
                        it.option(Option.createBuilder<Boolean>()
                            .name(Component.translatable("config.vibrancy.general.useMultithreading"))
                            .binding(true, VibrancyConfig::useMultithreading)
                            .controller(TickBoxControllerBuilder::create)
                            .build())
                    }
                }

                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("config.vibrancy.general.asyncThreads"))
                    .binding(4, VibrancyConfig::asyncThreads)
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(1, 8)
                            .step(1)
                    }
                    .build())

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.general.limitLightBrightness"))
                    .description(OptionDescription.of(
                        Component.translatable("config.vibrancy.general.limitLightBrightness.tooltip0"),
                        Component.translatable("config.vibrancy.general.limitLightBrightness.tooltip1")
                    ))
                    .binding(false, VibrancyConfig::limitLightBrightness)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.general.alignPixels"))
                    .description(OptionDescription.of(
                        Component.translatable("config.vibrancy.general.alignPixels.tooltip")
                    ))
                    .binding(true, VibrancyConfig::alignPixels)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.general.raycastLightModel"))
                    .description(OptionDescription.of(
                        Component.translatable("config.vibrancy.general.raycastLightModel.tooltip")
                    ))
                    .binding(false, VibrancyConfig::raycastLightModel)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.general.flickerStrength"))
                    .binding(1f, VibrancyConfig::flickerStrength)
                    .description(OptionDescription.of(
                        Component.translatable("config.vibrancy.blockLights.general.flickerStrength.tooltip")
                    ))
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0f, 2f)
                            .step(0.1f)
                    }
                    .build())
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.blockLights"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.vibrancy.blockLights.raytraced"))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.enabled"))
                        .binding(true, VibrancyConfig::rayLightsEnabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    /*
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.maxRendered"))
                        .binding(400, VibrancyConfig::rayLightsMaxRendered)
                        .description(OptionDescription.of(
                            Component.translatable("config.vibrancy.blockLights.raytraced.maxRendered.tooltip")
                        ))
                        .controller { opt ->
                            IntegerFieldControllerBuilder.create(opt)
                                .min(0)
                        }
                        .build())
                     */

                    .option(Option.createBuilder<Float>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.brightness"))
                        .binding(1f, VibrancyConfig::rayLightBrightness)
                        .description(OptionDescription.of(
                            Component.translatable("config.vibrancy.blockLights.raytraced.brightness.tooltip0"),
                            Component.translatable("config.vibrancy.blockLights.raytraced.brightness.tooltip1")
                        ))
                        .controller { opt ->
                            FloatSliderControllerBuilder.create(opt)
                                .range(0.1f, 2f)
                                .step(0.1f)
                        }
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.shadowRadius"))
                        .binding(6, VibrancyConfig::rayLightShadowRadius)
                        .description(OptionDescription.of(
                            Component.translatable("config.vibrancy.blockLights.raytraced.brightness.tooltip")
                        ))
                        .controller { opt ->
                            IntegerSliderControllerBuilder.create(opt)
                                .range(1, 16)
                                .step(1)
                        }
                        .build())

                    /*
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.maxHighQuality"))
                        .binding(10, VibrancyConfig::rayLightMaxHighQuality)
                        .description(OptionDescription.of(
                            Component.translatable("config.vibrancy.blockLights.raytraced.maxHighQuality.tooltip")
                        ))
                        .controller { opt ->
                            IntegerSliderControllerBuilder.create(opt)
                                .range(0, 30)
                                .step(5)
                        }
                        .build())
                     */
                    .build())

                /*
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.vibrancy.blockLights.subtle"))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.enabled"))
                        .binding(true, VibrancyConfig::subtleLightsEnabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.renderDistance"))
                        .binding(6, VibrancyConfig::subtleLightsRenderDistance)
                        .controller { opt ->
                            IntegerSliderControllerBuilder.create(opt)
                                .range(1, 16)
                                .step(1)
                        }
                        .build())

                    .option(Option.createBuilder<Float>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.brightness"))
                        .binding(1f, VibrancyConfig::subtleLightBrightness)
                        .description(OptionDescription.of(
                            Component.translatable("config.vibrancy.blockLights.subtle.brightness.tooltip0"),
                            Component.translatable("config.vibrancy.blockLights.subtle.brightness.tooltip1")
                        ))
                        .controller { opt ->
                            FloatSliderControllerBuilder.create(opt)
                                .range(0.1f, 2f)
                                .step(0.1f)
                        }
                        .build())

                    .option(Option.createBuilder<SubtleLightCullingMode>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.cullingMode"))
                        .binding(SubtleLightCullingMode.SOLID_NEIGHBOR, VibrancyConfig::subtleLightCullingMode)
                        .description(OptionDescription.of(
                            Component.translatable("config.vibrancy.blockLights.subtle.cullingMode.tooltip0"),
                            Component.translatable("config.vibrancy.blockLights.subtle.cullingMode.tooltip1"),
                            Component.translatable("config.vibrancy.blockLights.subtle.cullingMode.tooltip2")
                        ))
                        .controller { opt ->
                            EnumControllerBuilder.create(opt)
                                .enumClass(SubtleLightCullingMode::class.java)
                                .formatValue { Component.translatable(it.name.lowercase()) }
                        }
                        .build())
                    .build())
                 */

                .build())

                /*
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.skyLights"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.skyLights.enabled"))
                    .binding(true, VibrancyConfig::skyLightsEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("config.vibrancy.skyLights.shadow_distance"))
                    .binding(16, VibrancyConfig::skyLightShadowDistance)
                    .description(OptionDescription.of(
                        Component.translatable("config.vibrancy.skyLights.shadow_distance.tooltip")
                    ))
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(8, 32)
                            .step(4)
                    }
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.skyLights.brightness"))
                    .binding(1f, VibrancyConfig::skyLightBrightness)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0.1f, 2f)
                            .step(0.1f)
                    }
                    .build())

                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("config.vibrancy.skyLights.resolution"))
                    .binding(2, VibrancyConfig::skyLightResolution)
                    .description(OptionDescription.of(
                        Component.translatable("config.vibrancy.skyLights.resolution.tooltip")
                    ))
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(0, 3)
                            .step(1)
                            .formatValue { Component.literal((1 shl (it + 10)).toString()) }
                    }
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.skyLights.shadow_map_power"))
                    .binding(8f, VibrancyConfig::skyLightShadowMapPower)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(1f, 8f)
                            .step(0.5f)
                    }
                    .build())

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.skyLights.translucent_enabled"))
                    .binding(true, VibrancyConfig::skyLightTranslucentEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .build())
                 */

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.specularReflections"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.specularReflections.enabled"))
                    .binding(true, VibrancyConfig::reflectionsEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                /*
                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.specularReflections.strength"))
                    .binding(3.5f, VibrancyConfig::reflectionStrength)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0.5f, 10f)
                            .step(0.5f)
                    }
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.specularReflections.exponent"))
                    .binding(3f, VibrancyConfig::reflectionExponent)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0.5f, 10f)
                            .step(0.5f)
                    }
                    .build())
                 */
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.entityShadows"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.entityShadows.enabled"))
                    .binding(true, VibrancyConfig::entityShadowsEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                /*
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.entityShadows.blockEntityShadows"))
                    .binding(true, VibrancyConfig::blockEntityShadows)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("config.vibrancy.entityShadows.distance"))
                    .binding(3, VibrancyConfig::entityShadowDistance)
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(1, 16)
                            .step(1)
                    }
                    .build())

                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("config.vibrancy.entityShadows.maxLights"))
                    .binding(10, VibrancyConfig::entityShadowMaxBlockLights)
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(0, 100)
                            .step(5)
                    }
                    .build())
                 */
                .build())

            .build()
            .generateScreen(parent)
    }
}