package net.typho.vibrancy

import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.typho.big_shot_lib.api.client.rendering.opengl.GlQueue
import net.typho.big_shot_lib.api.util.platform.PlatformUtil
import net.typho.vibrancy.block.impl.SubtleLightCullingMode
import net.typho.vibrancy.util.VibrancyThreadPool
import java.nio.file.Files
import kotlin.reflect.KMutableProperty0

internal fun <T : Any> Option.Builder<T>.binding(property: KMutableProperty0<T>): Option.Builder<T> {
    return binding(property.get(), { property.get() }, { property.set(it) })
}

object VibrancyConfig {
    var modEnabled = true
        set(value) {
            field = value
            GlQueue.INSTANCE.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @JvmField
    var useMultithreading = true
    var asyncThreads: Int = 2
        set(value) {
            field = value
            VibrancyThreadPool.maximumPoolSize = value
            VibrancyThreadPool.corePoolSize = value
        }
    @JvmField
    var limitLightBrightness = false

    @JvmField
    var reflectionsEnabled = true
    @JvmField
    var reflectionStrength = 3.5f
    @JvmField
    var reflectionExponent = 3f

    @JvmField
    var entityShadowsEnabled = true
    @JvmField
    var entityShadowDistance = 2
    @JvmField
    var entityShadowMaxLights = 50

    var rayLightsEnabled = true
        set(value) {
            field = value
            GlQueue.INSTANCE.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @JvmField
    var rayLightsMaxRendered: Int = 400
    @JvmField
    var rayLightBrightness: Float = 1f
    @JvmField
    var rayLightShadowRadius: Int = 6

    var subtleLightsEnabled = true
        set(value) {
            field = value
            GlQueue.INSTANCE.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @JvmField
    var subtleLightsRenderDistance: Int = 6
    @JvmField
    var subtleLightBrightness = 1f
    var subtleLightCullingMode = SubtleLightCullingMode.SOLID_NEIGHBOR
        set(value) {
            field = value
            GlQueue.INSTANCE.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }

    @JvmStatic
    fun save() {
        JsonWriter(Files.newBufferedWriter(PlatformUtil.INSTANCE.configPath.resolve("vibrancy.json"))).use { writer ->
            writer.setIndent("    ")
            writer.beginObject()

                .name("modEnabled").value(modEnabled)
                .name("useMultithreading").value(useMultithreading)
                .name("asyncThreads").value(asyncThreads)
                .name("limitLightBrightness").value(limitLightBrightness)

                .name("specularReflections").beginObject()

                .name("enabled").value(reflectionsEnabled)
                .name("strength").value(reflectionStrength)
                .name("exponent").value(reflectionExponent)

                .endObject()

                .name("entityShadows").beginObject()

                .name("enabled").value(entityShadowsEnabled)
                .name("distance").value(entityShadowDistance)
                .name("maxLights").value(entityShadowMaxLights)

                .endObject()

                .name("blockLights").beginObject()

                .name("raytraced").beginObject()

                .name("enabled").value(rayLightsEnabled)
                .name("maxRendered").value(rayLightsMaxRendered)
                .name("brightness").value(rayLightBrightness)
                .name("shadowRadius").value(rayLightShadowRadius)

                .endObject()

                .name("subtle").beginObject()

                .name("enabled").value(subtleLightsEnabled)
                .name("renderDistance").value(subtleLightsRenderDistance)
                .name("brightness").value(subtleLightBrightness)
                .name("cullingMode").value(subtleLightCullingMode.name)

                .endObject()

                .endObject()

                .endObject()
        }
    }

    @JvmStatic
    fun load() {
        val json = Files.newBufferedReader(PlatformUtil.INSTANCE.configPath.resolve("vibrancy.json")).use { JsonParser.parseReader(it) }.asJsonObject

        json.getAsJsonPrimitive("modEnabled")?.let { modEnabled = it.asBoolean }
        json.getAsJsonPrimitive("useMultithreading")?.let { useMultithreading = it.asBoolean }
        json.getAsJsonPrimitive("asyncThreads")?.let { asyncThreads = it.asInt }
        json.getAsJsonPrimitive("limitLightBrightness")?.let { limitLightBrightness = it.asBoolean }

        json.getAsJsonObject("specularReflections")?.let { reflections ->
            reflections.getAsJsonPrimitive("enabled")?.let { reflectionsEnabled = it.asBoolean }
            reflections.getAsJsonPrimitive("strength")?.let { reflectionStrength = it.asFloat }
            reflections.getAsJsonPrimitive("exponent")?.let { reflectionExponent = it.asFloat }
        }

        json.getAsJsonObject("entityShadows")?.let { entityShadows ->
            entityShadows.getAsJsonPrimitive("enabled")?.let { entityShadowsEnabled = it.asBoolean }
            entityShadows.getAsJsonPrimitive("distance")?.let { entityShadowDistance = it.asInt }
            entityShadows.getAsJsonPrimitive("maxLights")?.let { entityShadowMaxLights = it.asInt }
        }

        json.getAsJsonObject("blockLights")?.let { blockLights ->
            blockLights.getAsJsonObject("raytraced")?.let { raytraced ->
                raytraced.getAsJsonPrimitive("enabled")?.let { rayLightsEnabled = it.asBoolean }
                raytraced.getAsJsonPrimitive("maxRendered")?.let { rayLightsMaxRendered = it.asInt }
                raytraced.getAsJsonPrimitive("brightness")?.let { rayLightBrightness = it.asFloat }
                raytraced.getAsJsonPrimitive("shadowRadius")?.let { rayLightShadowRadius = it.asInt }
            }

            blockLights.getAsJsonObject("subtle")?.let { subtle ->
                subtle.getAsJsonPrimitive("enabled")?.let { subtleLightsEnabled = it.asBoolean }
                subtle.getAsJsonPrimitive("renderDistance")?.let { subtleLightsRenderDistance = it.asInt }
                subtle.getAsJsonPrimitive("brightness")?.let { subtleLightBrightness = it.asFloat }
                subtle.getAsJsonPrimitive("cullingMode")?.let { subtleLightCullingMode = SubtleLightCullingMode.valueOf(it.asString.uppercase()) }
            }
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
                    .binding(VibrancyConfig::modEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.general.useMultithreading"))
                    .binding(VibrancyConfig::useMultithreading)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("config.vibrancy.general.asyncThreads"))
                    .binding(VibrancyConfig::asyncThreads)
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
                    .binding(VibrancyConfig::limitLightBrightness)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.specularReflections"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.specularReflections.enabled"))
                    .binding(VibrancyConfig::reflectionsEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.specularReflections.strength"))
                    .binding(VibrancyConfig::reflectionStrength)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0.5f, 10f)
                            .step(0.5f)
                    }
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.specularReflections.exponent"))
                    .binding(VibrancyConfig::reflectionExponent)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0.5f, 10f)
                            .step(0.5f)
                    }
                    .build())
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.blockLights"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.vibrancy.blockLights.raytraced"))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.enabled"))
                        .binding(VibrancyConfig::rayLightsEnabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.maxRendered"))
                        .binding(VibrancyConfig::rayLightsMaxRendered)
                        .description(OptionDescription.of(
                            Component.translatable("config.vibrancy.blockLights.raytraced.maxRendered.tooltip")
                        ))
                        .controller { opt ->
                            IntegerFieldControllerBuilder.create(opt)
                                .min(0)
                        }
                        .build())

                    .option(Option.createBuilder<Float>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.brightness"))
                        .binding(VibrancyConfig::rayLightBrightness)
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
                        .binding(VibrancyConfig::rayLightShadowRadius)
                        .description(OptionDescription.of(
                            Component.translatable("config.vibrancy.blockLights.raytraced.brightness.tooltip")
                        ))
                        .controller { opt ->
                            IntegerSliderControllerBuilder.create(opt)
                                .range(1, 16)
                                .step(1)
                        }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.vibrancy.blockLights.subtle"))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.enabled"))
                        .binding(VibrancyConfig::subtleLightsEnabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.renderDistance"))
                        .binding(VibrancyConfig::subtleLightsRenderDistance)
                        .controller { opt ->
                            IntegerSliderControllerBuilder.create(opt)
                                .range(1, 16)
                                .step(1)
                        }
                        .build())

                    .option(Option.createBuilder<Float>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.brightness"))
                        .binding(VibrancyConfig::subtleLightBrightness)
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
                        .binding(VibrancyConfig::subtleLightCullingMode)
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

                .build())

            .build()
            .generateScreen(parent)
    }
}