package net.typho.vibrancy

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import foundry.veil.api.client.registry.RenderTypeLayerRegistry
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder
import foundry.veil.api.client.render.rendertype.layer.LayerTemplateValue
import foundry.veil.api.client.render.rendertype.layer.RenderTypeLayer
import net.minecraft.client.renderer.RenderStateShard
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.GL_DECR_WRAP
import org.lwjgl.opengl.GL14.GL_INCR_WRAP

data class StencilLayer(
    val enabled: Boolean,
    val mask: Int,
    val func: StencilFunc,
    val op: StencilOp
) : RenderTypeLayer {
    override fun addShard(
        builder: VeilRenderTypeBuilder?,
        vararg params: Any?
    ) {
        builder?.addLayer(Shard(
            enabled,
            mask,
            func.func.parse(params).id,
            func.ref,
            func.mask,
            op.sfail.parse(params).id,
            op.dpfail.parse(params).id,
            op.dppass.parse(params).id
        ))
    }

    class Shard(
        val enabled: Boolean,
        val mask: Int,
        val funcFunc: Int,
        val funcRef: Int,
        val funcMask: Int,
        val opSfail: Int,
        val opDpfail: Int,
        val opDppass: Int
    ) : RenderStateShard(
        "vibrancy:stencil",
        {
            if (enabled) {
                glEnable(GL_STENCIL_TEST)
                glStencilMask(mask)
                glStencilFunc(
                    funcFunc,
                    funcRef,
                    funcMask
                )
                glStencilOp(
                    opSfail,
                    opDpfail,
                    opDppass
                )
            } else {
                glDisable(GL_STENCIL_TEST)
            }
        },
        {
            glDisable(GL_STENCIL_TEST)
        }
    )

    override fun getType(): RenderTypeLayerRegistry.LayerType<*>? = ModRenderTypeLayers.STENCIL

    companion object {
        val CODEC: MapCodec<StencilLayer> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.BOOL.fieldOf("enabled")
                    .forGetter { it.enabled },
                Codec.INT.optionalFieldOf("mask", 0xFFFFFFFF.toInt())
                    .forGetter { it.mask },
                StencilFunc.CODEC.codec().optionalFieldOf("func", StencilFunc.DEFAULT)
                    .forGetter { it.func },
                StencilOp.CODEC.codec().optionalFieldOf("op", StencilOp.DEFAULT)
                    .forGetter { it.op }
            ).apply(instance, ::StencilLayer)
        }
    }

    data class StencilFunc(
        val func: LayerTemplateValue<ComparisonFunction>,
        val ref: Int,
        val mask: Int
    ) {
        companion object {
            val DEFAULT = StencilFunc(LayerTemplateValue.raw(ComparisonFunction.ALWAYS), 0, 0xFF)
            val CODEC: MapCodec<StencilFunc> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    LayerTemplateValue.enumCodec(ComparisonFunction::class.java)
                        .fieldOf("func")
                        .forGetter { it.func },
                    Codec.INT.fieldOf("ref")
                        .forGetter { it.ref },
                    Codec.INT.fieldOf("mask")
                        .forGetter { it.mask },
                ).apply(instance, ::StencilFunc)
            }
        }
    }

    enum class ComparisonFunction(val id: Int) {
        NEVER(GL_NEVER),
        ALWAYS(GL_ALWAYS),
        LESS(GL_LESS),
        LEQUAL(GL_LEQUAL),
        EQUAL(GL_EQUAL),
        GEQUAL(GL_GEQUAL),
        GREATER(GL_GREATER),
        NOTEQUAL(GL_NOTEQUAL)
    }

    data class StencilOp(
        val sfail: LayerTemplateValue<TestAction>,
        val dpfail: LayerTemplateValue<TestAction>,
        val dppass: LayerTemplateValue<TestAction>
    ) {
        companion object {
            val DEFAULT = StencilOp(
                LayerTemplateValue.raw(TestAction.KEEP),
                LayerTemplateValue.raw(TestAction.KEEP),
                LayerTemplateValue.raw(TestAction.KEEP)
            )
            val CODEC: MapCodec<StencilOp> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    LayerTemplateValue.enumCodec(TestAction::class.java)
                        .fieldOf("sfail")
                        .forGetter { it.sfail },
                    LayerTemplateValue.enumCodec(TestAction::class.java)
                        .fieldOf("dpfail")
                        .forGetter { it.dpfail },
                    LayerTemplateValue.enumCodec(TestAction::class.java)
                        .fieldOf("dppass")
                        .forGetter { it.dppass }
                ).apply(instance, ::StencilOp)
            }
        }
    }

    enum class TestAction(val id: Int) {
        KEEP(GL_KEEP),
        ZERO(GL_ZERO),
        REPLACE(GL_REPLACE),
        INCR(GL_INCR),
        DECR(GL_DECR),
        INVERT(GL_INVERT),
        INCR_WRAP(GL_INCR_WRAP),
        DECR_WRAP(GL_DECR_WRAP)
    }
}