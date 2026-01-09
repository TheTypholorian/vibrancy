package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import org.joml.Vector2f
import org.joml.Vector3f

data class LightFace(
    val blockPos: BlockPos?,
    val vertex1: Vector3f, val vertex2: Vector3f, val vertex3: Vector3f, val vertex4: Vector3f,
    val sprite: TextureAtlasSprite?,
    val width: Int, val height: Int
) {
    fun buildGeometry(consumer: VertexConsumer) {
        consumer.addVertex(vertex1).setUv(sprite?.u0 ?: 0f, sprite?.v0 ?: 0f)
        consumer.addVertex(vertex2).setUv(sprite?.u1 ?: 1f, sprite?.v0 ?: 0f)
        consumer.addVertex(vertex3).setUv(sprite?.u1 ?: 1f, sprite?.v1 ?: 1f)
        consumer.addVertex(vertex4).setUv(sprite?.u0 ?: 0f, sprite?.v1 ?: 1f)
    }

    companion object {
        fun BakedQuad.toLightFace(x: Float, y: Float, z: Float, origin: BlockPos): LightFace {
            val vertices = arrayOfNulls<Vector3f>(4)
            val texCoords = arrayOfNulls<Vector2f>(4)
            val data = this.vertices
            val len = data.size / 8

            var j = 0
            for (i in 0 until len) {
                vertices[i] = Vector3f(
                    Float.fromBits(data[j]) + origin.x + x,
                    Float.fromBits(data[j + 1]) + origin.y + y,
                    Float.fromBits(data[j + 2]) + origin.z + z
                )
                texCoords[i] = Vector2f(
                    Float.fromBits(data[j + 4]),
                    Float.fromBits(data[j + 5])
                )

                j += 8
            }

            return LightFace(
                origin,
                vertices[0]!!,
                vertices[1]!!,
                vertices[2]!!,
                vertices[3]!!,
                sprite,
                1,
                1
            )
        }
    }
}