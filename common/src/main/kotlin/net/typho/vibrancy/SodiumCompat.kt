package net.typho.vibrancy

import com.google.common.collect.ImmutableList
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage
import net.minecraft.client.Options
import net.minecraft.network.chat.Component
import java.util.*

object SodiumCompat {
    fun rtxPage(vanillaOpts: MinecraftOptionsStorage): OptionPage {
        val groups = LinkedList<OptionGroup>()

        groups.add(
            OptionGroup.createBuilder()
                .add(
                    OptionImpl.createBuilder(Int::class.java, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.raytrace_distance"))
                        .setTooltip(Component.translatable("options.vibrancy.raytrace_distance.tooltip"))
                        .setControl {
                            SliderControl(
                                it,
                                1,
                                32,
                                1
                            ) { v -> Component.translatable("options.vibrancy.raytrace_distance.value", v * 16) }
                        }
                        .setBinding(
                            { options: Options, value: Int -> Vibrancy.LIGHT_MANAGER.raytraceDistance = value },
                            { options: Options -> Vibrancy.LIGHT_MANAGER.raytraceDistance }
                        )
                        .setImpact(OptionImpact.HIGH)
                        .build()
                )
                .add(
                    OptionImpl.createBuilder(Int::class.java, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.light_cull_distance"))
                        .setTooltip(Component.translatable("options.vibrancy.light_cull_distance.tooltip"))
                        .setControl {
                            SliderControl(
                                it,
                                1,
                                32,
                                1
                            ) { v -> Component.translatable("options.vibrancy.light_cull_distance.value", v * 16) }
                        }
                        .setBinding(
                            { options: Options, value: Int -> Vibrancy.LIGHT_MANAGER.lightCullDistance = value },
                            { options: Options -> Vibrancy.LIGHT_MANAGER.lightCullDistance }
                        )
                        .setImpact(OptionImpact.HIGH)
                        .build()
                )
                .add(
                    OptionImpl.createBuilder(Int::class.java, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.max_raytraced_lights"))
                        .setTooltip(Component.translatable("options.vibrancy.max_raytraced_lights.tooltip"))
                        .setControl {
                            SliderControl(
                                it,
                                0,
                                405,
                                5
                            ) { v ->
                                if (v > 400) {
                                    Component.translatable("options.vibrancy.max_raytraced_lights.max")
                                } else {
                                    Component.translatable("options.vibrancy.max_raytraced_lights.value", v)
                                }
                            }
                        }
                        .setBinding(
                            { options: Options, value: Int -> Vibrancy.LIGHT_MANAGER.maxRaytraced = value },
                            { options: Options -> Vibrancy.LIGHT_MANAGER.maxRaytraced }
                        )
                        .setImpact(OptionImpact.HIGH)
                        .build()
                )
                .add(
                    OptionImpl.createBuilder(Int::class.java, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.max_lights"))
                        .setTooltip(Component.translatable("options.vibrancy.max_lights.tooltip"))
                        .setControl {
                            SliderControl(
                                it,
                                0,
                                405,
                                5
                            ) { v ->
                                if (v > 400) {
                                    Component.translatable("options.vibrancy.max_lights.max")
                                } else {
                                    Component.translatable("options.vibrancy.max_lights.value", v)
                                }
                            }
                        }
                        .setBinding(
                            { options: Options, value: Int ->
                                Vibrancy.LIGHT_MANAGER.maxRendered = value
                                Vibrancy.LIGHT_MANAGER.maxRaytraced = Vibrancy.LIGHT_MANAGER.maxRaytraced.coerceAtMost(value)
                            },
                            { options: Options -> Vibrancy.LIGHT_MANAGER.maxRendered }
                        )
                        .setImpact(OptionImpact.HIGH)
                        .build()
                )
                .add(
                    OptionImpl.createBuilder(Int::class.java, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.shadow_radius"))
                        .setTooltip(Component.translatable("options.vibrancy.shadow_radius.tooltip"))
                        .setControl {
                            SliderControl(
                                it,
                                1,
                                16,
                                1
                            ) { v ->
                                if (v > 15) {
                                    Component.translatable("options.vibrancy.shadow_radius.max")
                                } else {
                                    Component.translatable("options.vibrancy.shadow_radius.value", v)
                                }
                            }
                        }
                        .setBinding(
                            { options: Options, value: Int -> Vibrancy.LIGHT_MANAGER.shadowRadius = value },
                            { options: Options -> Vibrancy.LIGHT_MANAGER.shadowRadius }
                        )
                        .setImpact(OptionImpact.HIGH)
                        .build()
                )
                .build()
        )

        return OptionPage(Component.translatable("options.vibrancy.page"), ImmutableList.copyOf(groups))
    }
}