package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.BlockPos
import org.joml.Vector2f
import org.joml.Vector3f

data class LightFace(
    val blockPos: BlockPos?,
    val vertex1: Vector3f, val vertex2: Vector3f, val vertex3: Vector3f, val vertex4: Vector3f,
    val texCoord1: Vector2f, val texCoord2: Vector2f, val texCoord3: Vector2f, val texCoord4: Vector2f,
    val width: Int, val height: Int
) {
    fun buildGeometry(consumer: VertexConsumer) {
        consumer.addVertex(vertex1).setUv(texCoord1.x, texCoord1.y)
        consumer.addVertex(vertex2).setUv(texCoord2.x, texCoord2.y)
        consumer.addVertex(vertex3).setUv(texCoord3.x, texCoord3.y)
        consumer.addVertex(vertex4).setUv(texCoord4.x, texCoord4.y)
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
                texCoords[0]!!,
                texCoords[1]!!,
                texCoords[2]!!,
                texCoords[3]!!,
                1,
                1
            )
        }
    }
}