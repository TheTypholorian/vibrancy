package net.typho.vibrancy

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.typho.vibrancy.block.impl.SubtleLightCullingMode
import kotlin.reflect.KMutableProperty0

fun <T : Any> Option.Builder<T>.binding(property: KMutableProperty0<T>): Option.Builder<T> {
    return binding(property.get(), { property.get() }, { property.set(it) })
}

object VibrancyConfig {
    @JvmField
    var modEnabled = true
    @JvmField
    var useMultithreading = true
    @JvmField
    var asyncThreads: Int = 2
    @JvmField
    var limitLightBrightness = false

    object SpecularReflectionsSection {
        @JvmField
        var enabled = true
        @JvmField
        var strength = 3.5f
        @JvmField
        var exponent = 3f
    }

    object BlockLightsSection {
        object RaytracedSection {
            @JvmField
            var enabled = true
            @JvmField
            var maxRendered: Int = 400
            @JvmField
            var brightness: Float = 1f
            @JvmField
            var shadowRadius: Int = 6
        }

        object SubtleSection {
            @JvmField
            var enabled = true
            @JvmField
            var renderDistance: Int = 6 // 50_000 to inf + 50_000
            @JvmField
            var brightness = 1f // 0.25 to 2.5 + 0.25 (%)
            @JvmField
            var cullingMode = SubtleLightCullingMode.SOLID_NEIGHBOR
        }
    }

    fun openScreen(parent: Screen?) {
        YetAnotherConfigLib.createBuilder()
            .title(Component.literal("config.vibrancy.title"))

            .category(ConfigCategory.createBuilder()
                .name(Component.literal("config.vibrancy.general"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("config.vibrancy.general.modEnabled"))
                    .binding(VibrancyConfig::modEnabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("config.vibrancy.general.useMultithreading"))
                    .binding(VibrancyConfig::useMultithreading)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Int>()
                    .name(Component.literal("config.vibrancy.general.asyncThreads"))
                    .binding(VibrancyConfig::asyncThreads)
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(1, 8)
                            .step(1)
                    }
                    .build())

                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("config.vibrancy.general.limitLightBrightness"))
                    .description(OptionDescription.of(
                        Component.literal("config.vibrancy.general.limitLightBrightness.tooltip0"),
                        Component.literal("config.vibrancy.general.limitLightBrightness.tooltip1")
                    ))
                    .binding(VibrancyConfig::limitLightBrightness)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.literal("config.vibrancy.specularReflections"))

                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("config.vibrancy.specularReflections.enabled"))
                    .binding(SpecularReflectionsSection::enabled)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.literal("config.vibrancy.specularReflections.strength"))
                    .binding(SpecularReflectionsSection::strength)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0.5f, 10f)
                            .step(0.5f)
                    }
                    .build())

                .option(Option.createBuilder<Float>()
                    .name(Component.literal("config.vibrancy.specularReflections.exponent"))
                    .binding(SpecularReflectionsSection::exponent)
                    .controller { opt ->
                        FloatSliderControllerBuilder.create(opt)
                            .range(0.5f, 10f)
                            .step(0.5f)
                    }
                    .build())
                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.literal("config.vibrancy.blockLights"))

                .group(OptionGroup.createBuilder()
                    .name(Component.literal("config.vibrancy.blockLights.raytraced"))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.literal("config.vibrancy.blockLights.raytraced.enabled"))
                        .binding(BlockLightsSection.RaytracedSection::enabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.literal("config.vibrancy.blockLights.raytraced.maxRendered"))
                        .binding(BlockLightsSection.RaytracedSection::maxRendered)
                        .controller { opt ->
                            IntegerFieldControllerBuilder.create(opt)
                                .min(0)
                        }
                        .build())
                    .build())

                .build())

            .build()
            .generateScreen(parent)
    }
}