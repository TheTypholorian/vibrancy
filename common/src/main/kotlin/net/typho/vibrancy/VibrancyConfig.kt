package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField.Companion.withListener
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.Companion.setFormat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.Companion.withIncrement
import net.minecraft.client.Minecraft
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.gl.InterpolationType
import java.text.DecimalFormat

class VibrancyConfig : Config(
    Vibrancy.id("config"),
    folder = "",
    name = Vibrancy.MOD_ID
) {
    override fun fileType() = FileType.JSON

    @JvmField
    var downscale = DownscaleSection()

    class DownscaleSection : ConfigSection() {
        @JvmField
        var factor = ValidatedInt(1, 32, 1)
            .withListener {
                RenderSystem.recordRenderCall {
                    val fbo = Minecraft.getInstance().mainRenderTarget
                    Vibrancy.SHADOW_FBO.resize(fbo.width, fbo.height)
                }
            }
        @JvmField
        var interpolation = ValidatedEnum(ConfigInterpolationType.LINEAR)
            .withListener {
                RenderSystem.recordRenderCall {
                    Vibrancy.SHADOW_FBO.colorAttachments.forEach { attachment ->
                        if (attachment is ITexture) {
                            attachment.setInterpolation(it.get().inner)
                        }
                    }
                }
            }
    }

    @JvmField
    var entityShadows = EntityShadowsSection()

    class EntityShadowsSection : ConfigSection() {
        @JvmField
        var firstPersonShadow = false
        @JvmField
        var distance = ValidatedInt(1, 16, 1)
            .setFormat(DecimalFormat("0 chunks"))
    }

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
            var entityShadows = false
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
        var circleShadowMultiplier = ValidatedFloat(1.5f, 2.5f, 0.25f)
            .withIncrement(0.25f)
            .setFormat(DecimalFormat("0%"))
    }

    enum class ConfigInterpolationType(val inner: InterpolationType) : EnumTranslatable {
        NEAREST(InterpolationType.NEAREST),
        LINEAR(InterpolationType.LINEAR);

        override fun prefix(): String {
            return "vibrancy.interpolation_type"
        }
    }
}