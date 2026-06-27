package net.typho.vibrancy

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.world.level.dimension.DimensionType

object TerrainLightTexture : LightTexture(Minecraft.getInstance().gameRenderer, Minecraft.getInstance()) {
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