package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.opengl.util.TexturedQuad
import net.typho.big_shot_lib.api.util.IColor
import org.joml.Vector3f

@JvmRecord
data class LightFace(
    @JvmField
    val blockPos: BlockPos,
    @JvmField
    val quad: TexturedQuad,
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) {
    fun buildGeometry(consumer: NeoVertexConsumer, level: Level?, offset: Vector3f = Vector3f()) {
        val tintColor = level?.let {
            IColor.RGBA(Minecraft.getInstance().blockColors.getColor(it.getBlockState(blockPos), level, blockPos, 0))
        } ?: IColor.FULL_ON

        consumer.vertex(quad.v1.add(offset, Vector3f())).textureUV(quad.uv1).color(tintColor)
        consumer.vertex(quad.v2.add(offset, Vector3f())).textureUV(quad.uv2).color(tintColor)
        consumer.vertex(quad.v3.add(offset, Vector3f())).textureUV(quad.uv3).color(tintColor)
        consumer.vertex(quad.v4.add(offset, Vector3f())).textureUV(quad.uv4).color(tintColor)
    }
}