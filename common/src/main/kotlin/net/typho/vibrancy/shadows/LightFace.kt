package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.BlockPos
import net.typho.vibrancy.util.TextureCoordinates
import org.joml.Vector2f
import org.joml.Vector3f

data class LightFace(
    val blockPos: BlockPos?,
    val vertex1: Vector3f, val vertex2: Vector3f, val vertex3: Vector3f, val vertex4: Vector3f,
    val texture: TextureCoordinates,
    val width: Int, val height: Int
) {
    fun buildGeometry(consumer: VertexConsumer) {
        consumer.addVertex(vertex1).setUv(texture.uv0.x, texture.uv0.y)
        consumer.addVertex(vertex2).setUv(texture.uv1.x, texture.uv1.y)
        consumer.addVertex(vertex3).setUv(texture.uv2.x, texture.uv2.y)
        consumer.addVertex(vertex4).setUv(texture.uv3.x, texture.uv3.y)
    }

    fun buildLines(consumer: VertexConsumer, offset: Vector3f, origin: Vector3f, length: Float) {
        fun line(v1: Vector3f, v2: Vector3f, r: Float, g: Float, b: Float) {
            consumer.addVertex(Vector3f(v1).add(offset)).setColor(r, g, b, 1f).setNormal(0f, 1f, 0f)
            consumer.addVertex(Vector3f(v2).add(offset)).setColor(r, g, b, 1f).setNormal(0f, 1f, 0f)
        }

        fun projectedLine(v: Vector3f) {
            line(v, Vector3f(origin).add(Vector3f(v).sub(origin).normalize(length)), 0f, 0f, 1f)
        }

        line(vertex1, vertex2, 0f, 1f, 0f)
        line(vertex2, vertex3, 0f, 1f, 0f)
        line(vertex3, vertex4, 0f, 1f, 0f)
        line(vertex4, vertex1, 0f, 1f, 0f)

        line(vertex1, vertex3, 1f, 0f, 0f)

        projectedLine(vertex1)
        projectedLine(vertex2)
        projectedLine(vertex3)
        projectedLine(vertex4)
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
                TextureCoordinates(texCoords),
                1,
                1
            )
        }
    }
}