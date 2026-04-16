package net.typho.vibrancy

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.*
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.client.rendering.opengl.GlQueue
import net.typho.big_shot_lib.api.util.platform.PlatformUtil
import net.typho.vibrancy.block.impl.SubtleLightCullingMode
import net.typho.vibrancy.util.VibrancyThreadPool
import kotlin.reflect.KMutableProperty0

internal fun <T : Any> Option.Builder<T>.binding(property: KMutableProperty0<T>): Option.Builder<T> {
    return binding(property.get(), { property.get() }, { property.set(it) })
}

class VibrancyConfig {
    @SerialEntry
    var modEnabled = true
        set(value) {
            field = value
            GlQueue.INSTANCE.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @SerialEntry
    @JvmField
    var useMultithreading = true
    @SerialEntry
    var asyncThreads: Int = 2
        set(value) {
            field = value
            VibrancyThreadPool.maximumPoolSize = value
            VibrancyThreadPool.corePoolSize = value
        }
    @SerialEntry
    @JvmField
    var limitLightBrightness = false

    @SerialEntry
    @JvmField
    var reflectionsEnabled = true
    @SerialEntry
    @JvmField
    var reflectionStrength = 3.5f
    @SerialEntry
    @JvmField
    var reflectionExponent = 3f

    @SerialEntry
    @JvmField
    var entityShadowsEnabled = true
    @SerialEntry
    @JvmField
    var entityShadowDistance = 2
    @SerialEntry
    @JvmField
    var entityShadowMaxLights = 50

    @SerialEntry
    var rayLightsEnabled = true
        set(value) {
            field = value
            GlQueue.INSTANCE.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @SerialEntry
    @JvmField
    var rayLightsMaxRendered: Int = 400
    @SerialEntry
    @JvmField
    var rayLightBrightness: Float = 1f
    @SerialEntry
    @JvmField
    var rayLightShadowRadius: Int = 6

    @SerialEntry
    var subtleLightsEnabled = true
        set(value) {
            field = value
            GlQueue.INSTANCE.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }
    @SerialEntry
    @JvmField
    var subtleLightsRenderDistance: Int = 6
    @SerialEntry
    @JvmField
    var subtleLightBrightness = 1f
    @SerialEntry
    var subtleLightCullingMode = SubtleLightCullingMode.SOLID_NEIGHBOR
        set(value) {
            field = value
            GlQueue.INSTANCE.runOrQueue {
                Vibrancy.lightManager.reload()
            }
        }

    companion object {
        @JvmField
        val HANDLER: ConfigClassHandler<VibrancyConfig> = ConfigClassHandler.createBuilder(VibrancyConfig::class.java)
            .id(ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "config")) // TODO fix
            .serializer { handler -> GsonConfigSerializerBuilder.create(handler)
                .setPath(PlatformUtil.INSTANCE.configPath.resolve("vibrancy.json"))
                .build() }
            .build()

        operator fun invoke(): VibrancyConfig = HANDLER.instance()

        @JvmStatic
        fun createScreen(parent: Screen?, config: VibrancyConfig = HANDLER.instance()): Screen {
            return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.vibrancy.title"))
                .save { HANDLER.save() }

                .category(ConfigCategory.createBuilder()
                    .name(Component.translatable("config.vibrancy.general"))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.vibrancy.general.modEnabled"))
                        .binding(config::modEnabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.vibrancy.general.useMultithreading"))
                        .binding(config::useMultithreading)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.general.asyncThreads"))
                        .binding(config::asyncThreads)
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
                        .binding(config::limitLightBrightness)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())

                .category(ConfigCategory.createBuilder()
                    .name(Component.translatable("config.vibrancy.specularReflections"))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.vibrancy.specularReflections.enabled"))
                        .binding(config::reflectionsEnabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Float>()
                        .name(Component.translatable("config.vibrancy.specularReflections.strength"))
                        .binding(config::reflectionStrength)
                        .controller { opt ->
                            FloatSliderControllerBuilder.create(opt)
                                .range(0.5f, 10f)
                                .step(0.5f)
                        }
                        .build())

                    .option(Option.createBuilder<Float>()
                        .name(Component.translatable("config.vibrancy.specularReflections.exponent"))
                        .binding(config::reflectionExponent)
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
                            .binding(config::rayLightsEnabled)
                            .controller(TickBoxControllerBuilder::create)
                            .build())

                        .option(Option.createBuilder<Int>()
                            .name(Component.translatable("config.vibrancy.blockLights.raytraced.maxRendered"))
                            .binding(config::rayLightsMaxRendered)
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
                            .binding(config::rayLightBrightness)
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
                            .binding(config::rayLightShadowRadius)
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
                            .binding(config::subtleLightsEnabled)
                            .controller(TickBoxControllerBuilder::create)
                            .build())

                        .option(Option.createBuilder<Int>()
                            .name(Component.translatable("config.vibrancy.blockLights.subtle.renderDistance"))
                            .binding(config::subtleLightsRenderDistance)
                            .controller { opt ->
                                IntegerSliderControllerBuilder.create(opt)
                                    .range(1, 16)
                                    .step(1)
                            }
                            .build())

                        .option(Option.createBuilder<Float>()
                            .name(Component.translatable("config.vibrancy.blockLights.subtle.brightness"))
                            .binding(config::subtleLightBrightness)
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
                            .binding(config::subtleLightCullingMode)
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
}