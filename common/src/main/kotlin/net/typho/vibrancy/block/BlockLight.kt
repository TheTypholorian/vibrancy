package net.typho.vibrancy.block

import foundry.veil.api.client.color.Color
import foundry.veil.api.client.color.Colorc
import net.minecraft.core.BlockPos
import net.typho.vibrancy.api.PointLight
import org.joml.Vector3f

data class BlockLight(
    val blockPos: BlockPos,
    private val radius: Float,
    private val color: Colorc
) : PointLight() {
    override fun getPosition(): Vector3f = blockPos.center.toVector3f()

    override fun getRadius(): Float = radius

    override fun getColor(): Colorc = color

    companion object {
        val LIGHTS = HashMap<BlockPos, BlockLight>()

        init {
            LIGHTS.put(BlockPos(0, 0, 0), BlockLight(BlockPos(0, 0, 0), 15f, Color.GREEN))
        }
    }
}