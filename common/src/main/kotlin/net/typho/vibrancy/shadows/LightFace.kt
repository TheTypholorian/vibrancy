package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.typho.big_shot_lib.api.client.rendering.meshes.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.rendering.meshes.TexturedQuad
import org.joml.Vector3f

@JvmRecord
data class LightFace(
    val blockPos: BlockPos?,
    val quad: TexturedQuad,
    val width: Int, val height: Int
) {
    fun buildGeometry(consumer: NeoVertexConsumer, offset: Vector3f = Vector3f()) {
        consumer.vertex(quad.v1.add(offset, Vector3f())).textureUV(quad.uv1)
        consumer.vertex(quad.v2.add(offset, Vector3f())).textureUV(quad.uv2)
        consumer.vertex(quad.v3.add(offset, Vector3f())).textureUV(quad.uv3)
        consumer.vertex(quad.v4.add(offset, Vector3f())).textureUV(quad.uv4)
    }
}