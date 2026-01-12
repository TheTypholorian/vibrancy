package net.typho.vibrancy

import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.annotations.RequiresAction
import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt

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
        @RequiresAction(Action.RELOG)
        var enabled = true
        @JvmField
        var brightness = ValidatedFloat(1f, 2f, 0.25f)
        @JvmField
        var entityShadows = true
        @JvmField
        var raytraceDistance = ValidatedInt(8, 64, 1)
        @JvmField
        var lightCullDistance = ValidatedInt(32, 64, 1)
        @JvmField
        var shadowRadius = 8
        @JvmField
        var maxRendered = 200
        @JvmField
        var maxRaytraced = 100
    }

    @JvmField
    var skyLights = SkyLightsSection()

    class SkyLightsSection : ConfigSection() {
        @JvmField
        @RequiresAction(Action.RELOG)
        var enabled = true
    }

    @JvmField
    var forNerds = ForNerdsSection()

    class ForNerdsSection : ConfigSection() {
        @JvmField
        var useFrustumCulling = true
        @JvmField
        var useGreedyMeshing = true
    }

    override fun onUpdateClient() {
        Vibrancy.LIGHT_MANAGER.rebuildAllShadows()
    }
}