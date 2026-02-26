package net.typho.vibrancy

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

@Config(name = Vibrancy.MOD_ID)
class VibrancyConfig : ConfigData {
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
            @ConfigEntry.Gui.Tooltip(count = 2)
            var brightness: Float = 1.25f // 0.25 to 2.5 + 0.25 (%)
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = 32)
            var raytraceDistance: Int = 8 // 8 to 64 + 4 (chunks)
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = 32)
            var renderDistance: Int = 32 // 4 to 64 + 4 (chunks)
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
            var shadowRadius: Int = 8 // 1 to 16 + 1
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = 32)
            var foregroundDistance: Int = 2
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = Long.MAX_VALUE)
            var maxForeground: Int = 25
            @JvmField
            @ConfigEntry.Gui.Tooltip(count = 3)
            @ConfigEntry.BoundedDiscrete(min = 16, max = 256)
            var backgroundShadowQuality: Int = 48 // 16 to 256 + 16
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = Long.MAX_VALUE)
            var maxRendered: Int = 400 // 200 to 1000 + 25
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = Long.MAX_VALUE)
            var maxRaytraced: Int = 200 // 100 to 500 + 25
        }

        @JvmField
        @ConfigEntry.Gui.CollapsibleObject
        var subtle = SubtleSection()

        class SubtleSection : ConfigData {
            @JvmField
            var enabled = true
            @JvmField
            @ConfigEntry.Gui.Tooltip(count = 2)
            var brightness = 1.25f // 0.25 to 2.5 + 0.25 (%)
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = 32)
            var renderDistance: Int = 32 // 4 to 64 + 4 (chunks)
            @JvmField
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.BoundedDiscrete(min = 1, max = Long.MAX_VALUE)
            var maxRendered: Int = 200_000 // 50_000 to inf + 50_000
        }
    }
}