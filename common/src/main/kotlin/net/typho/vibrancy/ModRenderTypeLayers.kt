package net.typho.vibrancy

import foundry.veil.api.client.registry.RenderTypeLayerRegistry
import net.minecraft.core.Registry

object ModRenderTypeLayers {
    fun init() = Unit

    val STENCIL: RenderTypeLayerRegistry.LayerType<StencilLayer?> = Registry.register(
        RenderTypeLayerRegistry.REGISTRY,
        Vibrancy.id("stencil"),
        RenderTypeLayerRegistry.LayerType(StencilLayer.CODEC)
    )
}