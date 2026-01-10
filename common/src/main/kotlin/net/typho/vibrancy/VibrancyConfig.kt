package net.typho.vibrancy

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

    var visuals = VisualsSection()

    class VisualsSection : ConfigSection() {
        var lightBrightness = ValidatedFloat(1f, 2f, 0.5f)
        var entityShadows = true
    }

    var blockLights = BlockLightsSection()

    class BlockLightsSection : ConfigSection() {
        var raytraceDistance = ValidatedInt(16, 32, 1)
        var lightCullDistance = ValidatedInt(32, 32, 1)
        var shadowRadius = 5
        var maxRendered = 200
        var maxRaytraced = 100
    }

    var skyLights = SkyLightsSection()

    class SkyLightsSection : ConfigSection() {
    }

    var forNerds = ForNerdsSection()

    class ForNerdsSection : ConfigSection() {
        var useFrustumCulling = true
        var useGreedyMeshing = true
    }

    override fun onUpdateClient() {
        Vibrancy.reloadShadows()
    }
}