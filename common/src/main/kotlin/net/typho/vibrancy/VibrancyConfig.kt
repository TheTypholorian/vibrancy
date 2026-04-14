package net.typho.vibrancy

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry
import net.typho.vibrancy.block.impl.SubtleLightCullingMode

@Config(name = Vibrancy.MOD_ID)
class VibrancyConfig : ConfigData {
    @JvmField
    var modEnabled = true
    @JvmField
    var useMultithreading = true
    @JvmField
    var asyncThreads: Int = 2
    @JvmField
    @ConfigEntry.Gui.Tooltip(count = 2)
    var limitLightBrightness = false
    @JvmField
    @ConfigEntry.Gui.CollapsibleObject
    var specularReflections = SpecularReflectionsSection()

    class SpecularReflectionsSection : ConfigData {
        @JvmField
        var enabled = true
        @JvmField
        var strength = 3.5f
        @JvmField
        var exponent = 3f
    }

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
            @JvmField
            @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
            var cullingMode = SubtleLightCullingMode.SOLID_NEIGHBOR
        }
    }
}