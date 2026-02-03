package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.annotations.RequiresAction
import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField.Companion.withListener
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
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
            var raytraceDistance = ValidatedInt(8, 64, 4)
                .withIncrement(4)
                .setFormat(DecimalFormat("0 chunks"))
            @JvmField
            var renderDistance = ValidatedInt(32, 64, 4)
                .withIncrement(4)
                .setFormat(DecimalFormat("0 chunks"))
            @JvmField
            var shadowRadius = ValidatedInt(8, 16, 1)
                .withListener {
                    RenderSystem.recordRenderCall {
                        Vibrancy.LIGHT_MANAGER.rebuildAllShadows()
                    }
                }
            @RequiresAction(Action.RELOG)
            @JvmField
            var shadowTextureWidth = ValidatedInt(400, 3200, 100)
                .withIncrement(100)
            @RequiresAction(Action.RELOG)
            @JvmField
            var shadowTextureHeight = ValidatedInt(600, 3200, 100)
                .withIncrement(100)
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
                .setFormat(DecimalFormat("0 chunks"))
            @JvmField
            var maxRendered = ValidatedInt(500_000, Int.MAX_VALUE, 0, ValidatedNumber.WidgetType.TEXTBOX_WITH_BUTTONS)
                .withIncrement(50_000)
        }
    }

    @JvmField
    var forNerds = ForNerdsSection()

    class ForNerdsSection : ConfigSection() {
        @JvmField
        var useFrustumCulling = true
    }
}