package net.typho.vibrancy

import net.minecraft.client.renderer.Lightmap
import net.minecraft.world.level.dimension.DimensionType

object TerrainLightTexture : Lightmap() {
    @JvmField
    val inUse = ThreadLocal<Boolean>()

    @JvmStatic
    fun isInUse() = inUse.get() ?: false

    fun getSkyBrightness(dimension: DimensionType, x: Int): Float {
        return getBrightness(dimension, x)
    }

    fun getBlockBrightness(dimension: DimensionType, x: Int): Float {
        val f = getBrightness(dimension, x)
        return f//.pow(0.75f) * 0.75f
    }
}