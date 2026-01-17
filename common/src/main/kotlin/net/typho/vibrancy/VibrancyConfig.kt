package net.typho.vibrancy

import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField.Companion.withListener
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.Companion.setFormat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.Companion.withIncrement
import java.text.DecimalFormat

class VibrancyConfig : Config(
    Vibrancy.id("config"),
    folder = "",
    name = Vibrancy.MOD_ID
) {
    override fun fileType() = FileType.JSON

    @JvmField
    var blockLights = BlockLightsSection()

    class BlockLightsSection : ConfigSection() {
        @JvmField
        var raytraced = RaytracedSection()

        class RaytracedSection : ConfigSection() {
            @JvmField
            var enabled = true
            @JvmField
            var brightness = ValidatedFloat(1.25f, 2.5f, 0.25f)
                .withIncrement(0.05f)
                .setFormat(DecimalFormat("0%"))
            @JvmField
            var entityShadows = true
            @JvmField
            var raytraceDistance = ValidatedInt(8, 64, 4)
                .withIncrement(4)
            @JvmField
            var renderDistance = ValidatedInt(32, 64, 4)
                .withIncrement(4)
            @JvmField
            var shadowRadius = ValidatedInt(8, 16, 1)
                .withListener { Vibrancy.LIGHT_MANAGER.rebuildAllShadows() }
            @JvmField
            var maxRendered = ValidatedInt(200, 1000, 0)
                .withIncrement(10)
            @JvmField
            var maxRaytraced = ValidatedInt(100, 500, 0)
                .withIncrement(10)
        }

        @JvmField
        var subtle = SubtleSection()

        class SubtleSection : ConfigSection() {
            @JvmField
            var enabled = true
            @JvmField
            var brightness = ValidatedFloat(1f, 2.5f, 0.25f)
                .withIncrement(0.05f)
                .setFormat(DecimalFormat("0%"))
            @JvmField
            var renderDistance = ValidatedInt(32, 64, 4)
                .withIncrement(4)
            @JvmField
            var maxRendered = ValidatedInt(500_000, Int.MAX_VALUE, 0)
                .withIncrement(50_000)
        }
    }

    @JvmField
    var skyLights = SkyLightsSection()

    class SkyLightsSection : ConfigSection() {
        @JvmField
        var enabled = true
    }

    @JvmField
    var forNerds = ForNerdsSection()

    class ForNerdsSection : ConfigSection() {
        @JvmField
        var useFrustumCulling = true
        @JvmField
        var useGreedyMeshing = ValidatedBoolean(true)
            .withListener { Vibrancy.LIGHT_MANAGER.rebuildAllShadows() }
        @JvmField
        var useExtraGreedyMeshing = ValidatedBoolean(true)
            .withListener { Vibrancy.LIGHT_MANAGER.rebuildAllShadows() }
    }
}