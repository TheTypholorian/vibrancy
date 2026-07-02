package net.typho.vibrancy.entity

import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.feature.FeatureFrameContext
import net.minecraft.client.renderer.feature.FeatureRendererType
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer
import net.minecraft.client.renderer.feature.submit.SubmitNode
import net.minecraft.core.BlockBox

open class VibrancyEntityShadowFeatureRenderer : RenderTypeFeatureRenderer<VibrancyEntityShadowFeatureRenderer.Submit<*>>() {
    companion object {
        @JvmField
        val TYPE = FeatureRendererType.create<Submit<*>>("Vibrancy Entity Shadows")
    }

    override fun buildGroup(
        context: FeatureFrameContext,
        submits: List<Submit<*>>
    ) {
    }

    open class Storage : SubmitNodeStorage() {
    }

    open class Submit<S : EntityRenderState>(
        @JvmField
        val renderer: EntityRenderer<*, S>,
        @JvmField
        val state: S,
        @JvmField
        val boundingBox: BlockBox
    ) : SubmitNode {
        override fun featureType(): FeatureRendererType<out Submit<*>> {
            return TYPE
        }
    }
}