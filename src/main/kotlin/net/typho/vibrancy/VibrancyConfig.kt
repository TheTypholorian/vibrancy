package net.typho.vibrancy

import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.nio.file.Files
import kotlin.reflect.KMutableProperty0

internal fun <T : Any> Option.Builder<T>.binding(def: T, property: KMutableProperty0<T>): Option.Builder<T> {
    return binding(def, { property.get() }, { property.set(it) })
}

object VibrancyConfig {
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
    var rayLightBrightness = 1f

    @JvmField
    var skyLightsEnabled = true
    @JvmField
    var skyLightBrightness = 1f

    @JvmStatic
    fun save() {
        JsonWriter(Files.newBufferedWriter(FabricLoader.getInstance().configDir.resolve("vibrancy.json"))).use { writer ->
            writer.setIndent("    ")
            writer.beginObject()

                .name("limitLightBrightness").value(limitLightBrightness)

                .name("specularReflections").beginObject()
                .name("enabled").value(reflectionsEnabled)
                .name("strength").value(reflectionStrength)
                .name("exponent").value(reflectionExponent)
                .endObject()

                .name("entityShadows").beginObject()
                .name("enabled").value(entityShadowsEnabled)
                .endObject()

                .name("blockLights").beginObject()

                .name("raytraced").beginObject()
                .name("brightness").value(rayLightBrightness)
                .endObject()

                .endObject()

                .name("skyLights").beginObject()
                .name("enabled").value(skyLightsEnabled)
                .name("brightness").value(skyLightBrightness)
                .endObject()

                .endObject()
        }
    }

    @JvmStatic
    fun load() {
        val path = FabricLoader.getInstance().configDir.resolve("vibrancy.json")

        if (Files.exists(path)) {
            try {
                val json = Files.newBufferedReader(path).use { JsonParser.parseReader(it) }.asJsonObject

                json.getAsJsonPrimitive("limitLightBrightness")?.let { limitLightBrightness = it.asBoolean }

                json.getAsJsonObject("specularReflections")?.let { reflections ->
                    reflections.getAsJsonPrimitive("enabled")?.let { reflectionsEnabled = it.asBoolean }
                    reflections.getAsJsonPrimitive("strength")?.let { reflectionStrength = it.asFloat }
                    reflections.getAsJsonPrimitive("exponent")?.let { reflectionExponent = it.asFloat }
                }

                json.getAsJsonObject("entityShadows")?.let { entityShadows ->
                    entityShadows.getAsJsonPrimitive("enabled")?.let { entityShadowsEnabled = it.asBoolean }
                }

                json.getAsJsonObject("blockLights")?.let { blockLights ->
                    blockLights.getAsJsonObject("raytraced")?.let { raytraced ->
                        raytraced.getAsJsonPrimitive("brightness")?.let { rayLightBrightness = it.asFloat }
                    }
                }

                json.getAsJsonObject("skyLights")?.let { skyLights ->
                    skyLights.getAsJsonPrimitive("enabled")?.let { skyLightsEnabled = it.asBoolean }
                    skyLights.getAsJsonPrimitive("brightness")?.let { skyLightBrightness = it.asFloat }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    .name(Component.translatable("config.vibrancy.general.limitLightBrightness"))
                    .description(OptionDescription.of(
                        Component.translatable("config.vibrancy.general.limitLightBrightness.tooltip0"),
                        Component.translatable("config.vibrancy.general.limitLightBrightness.tooltip1")
                    ))
                    .binding(false, VibrancyConfig::limitLightBrightness)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.blockLights"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.vibrancy.blockLights.raytraced"))

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
                    .build())

                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.skyLights"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.skyLights.enabled"))
                    .binding(true, VibrancyConfig::skyLightsEnabled)
                    .controller(TickBoxControllerBuilder::create)
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
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.specularReflections"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.specularReflections.enabled"))
                    .binding(true, VibrancyConfig::reflectionsEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

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
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.vibrancy.entityShadows"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("config.vibrancy.entityShadows.enabled"))
                    .binding(true, VibrancyConfig::entityShadowsEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .build())

            .build()
            .generateScreen(parent)
    }
}