package net.typho.vibrancy

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
import net.minecraft.client.renderer.chunk.ChunkSectionLayer

object VibrancyRenderPasses {
    @JvmField
    val SOLID = TerrainRenderPass(ChunkSectionLayer.SOLID, false, false)
    @JvmField
    val CUTOUT = TerrainRenderPass(ChunkSectionLayer.CUTOUT, false, true)
    @JvmField
    val TRANSLUCENT = TerrainRenderPass(ChunkSectionLayer.TRANSLUCENT, true, true)
    @JvmField
    val ALL = arrayOf(SOLID, CUTOUT, TRANSLUCENT)
    @JvmField
    val MAP = mapOf(
        DefaultTerrainRenderPasses.SOLID to SOLID,
        DefaultTerrainRenderPasses.CUTOUT to CUTOUT,
        DefaultTerrainRenderPasses.TRANSLUCENT to TRANSLUCENT,
    )
}