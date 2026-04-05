package net.typho.vibrancy

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

@Config(name = Vibrancy.MOD_ID)
class VibrancyConfig : ConfigData {
    @JvmField
    var useMultithreading = true

    @JvmField
    @ConfigEntry.Gui.Tooltip
    var limitLightBrightness = false

    @JvmField
    @ConfigEntry.Gui.CollapsibleObject
    var blockLights = BlockLightsSection()

    class BlockLightsSection : ConfigData {
        @JvmField
        @ConfigEntry.Gui.CollapsibleObject
        var raytraced = RaytracedSection()

        class RaytracedSection : ConfigData {
            @JvmField
            var enabled = true
            @JvmField
            @ConfigEntry.Gui.Tooltip
            var maxRendered: Int = 400
            @JvmField
            @ConfigEntry.Gui.Tooltip(count = 2)
            var brightness: Float = 1f
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
            var lightRadius: Int = 12
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
            var shadowRadius: Int = 6
        }

        @JvmField
        @ConfigEntry.Gui.CollapsibleObject
        var subtle = SubtleSection()

        class SubtleSection : ConfigData {
            @JvmField
            var enabled = true
            @JvmField
            var renderDistance: Int = 6 // 50_000 to inf + 50_000
            @JvmField
            @ConfigEntry.Gui.Tooltip(count = 2)
            var brightness = 1f // 0.25 to 2.5 + 0.25 (%)
        }
    }
}