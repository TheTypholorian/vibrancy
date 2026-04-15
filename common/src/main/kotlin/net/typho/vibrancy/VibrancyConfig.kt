package net.typho.vibrancy

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.typho.big_shot_lib.api.client.rendering.opengl.GlQueue
import net.typho.vibrancy.block.impl.SubtleLightCullingMode
import net.typho.vibrancy.util.VibrancyThreadPool
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

    object SpecularReflections {
        @JvmField
        var enabled = true
        @JvmField
        var strength = 3.5f
        @JvmField
        var exponent = 3f
    }

    object BlockLights {
        object Raytraced {
            var enabled = true
                set(value) {
                    field = value
                    GlQueue.INSTANCE.runOrQueue {
                        Vibrancy.lightManager.reload()
                    }
                }
            @JvmField
            var maxRendered: Int = 400
            @JvmField
            var brightness: Float = 1f
            @JvmField
            var shadowRadius: Int = 6
        }

        object Subtle {
            var enabled = true
                set(value) {
                    field = value
                    GlQueue.INSTANCE.runOrQueue {
                        Vibrancy.lightManager.reload()
                    }
                }
            @JvmField
            var renderDistance: Int = 6
            @JvmField
            var brightness = 1f
            var cullingMode = SubtleLightCullingMode.SOLID_NEIGHBOR
                set(value) {
                    field = value
                    GlQueue.INSTANCE.runOrQueue {
                        Vibrancy.lightManager.reload()
                    }
                }
        }
    }

    fun createScreen(parent: Screen?): Screen {
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("config.vibrancy.title"))

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
                    .binding(SpecularReflections::enabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.specularReflections.strength"))
                    .binding(SpecularReflections::strength)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0.5f, 10f)
                            .step(0.5f)
                    }
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.translatable("config.vibrancy.specularReflections.exponent"))
                    .binding(SpecularReflections::exponent)
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
                        .binding(BlockLights.Raytraced::enabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.blockLights.raytraced.maxRendered"))
                        .binding(BlockLights.Raytraced::maxRendered)
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
                        .binding(BlockLights.Raytraced::brightness)
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
                        .binding(BlockLights.Raytraced::shadowRadius)
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
                        .binding(BlockLights.Subtle::enabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.renderDistance"))
                        .binding(BlockLights.Subtle::renderDistance)
                        .controller { opt ->
                            IntegerSliderControllerBuilder.create(opt)
                                .range(1, 16)
                                .step(1)
                        }
                        .build())

                    .option(Option.createBuilder<Float>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.brightness"))
                        .binding(BlockLights.Subtle::brightness)
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

                    .option(Option.createBuilder<SubtleLightCullingMode>()
                        .name(Component.translatable("config.vibrancy.blockLights.subtle.cullingMode"))
                        .binding(BlockLights.Subtle::cullingMode)
                        .controller { opt ->
                            EnumDropdownControllerBuilder.create(opt)
                        }
                        .build())
                    .build())

                .build())

            .build()
            .generateScreen(parent)
    }
}