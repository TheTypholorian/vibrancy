package net.typho.vibrancy.api

import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.ByteBuffer

data class LightFace(
    var blockPos: BlockPos?, var direction: Direction?, var relative: BlockPos?,
    var vertex1: Vector3f, var vertex2: Vector3f, var vertex3: Vector3f, var vertex4: Vector3f,
    var texCoord1: Vector2f, var texCoord2: Vector2f, var texCoord3: Vector2f, var texCoord4: Vector2f,
    var normal: Vector3f, var dot: Float,
    var diagonal1: Vector3f, var diagonal2: Vector3f,
    var inverse: Vector4f?
) : LightFaceConvertible {
    constructor(
        blockPos: BlockPos, direction: Direction?, v1: Vector3f, v2: Vector3f, v3: Vector3f, v4: Vector3f,
        uv1: Vector2f, uv2: Vector2f, uv3: Vector2f, uv4: Vector2f
    ) : this(
        blockPos, direction, if (direction == null) null else blockPos.relative(direction),
        v1, v2, v3, v4, uv1, uv2, uv3, uv4,
        Vector3f(v2).sub(v1).cross(Vector3f(v4).sub(v1)).normalize(),
        Vector3f(v2).sub(v1).cross(Vector3f(v4).sub(v1)).normalize().dot(v1),
        Vector3f(v2).sub(v1),
        Vector3f(v4).sub(v1),
        null
    )

    init {
        val d11 = diagonal1.dot(diagonal1)
        val d12 = diagonal1.dot(diagonal2)
        val d22 = diagonal2.dot(diagonal2)
        val invDet = 1.0f / (d11 * d22 - d12 * d12)

        val inv11 = d22 * invDet
        val inv12 = -d12 * invDet
        val inv21 = -d12 * invDet
        val inv22 = d11 * invDet

        inverse = Vector4f(inv11, inv12, inv21, inv22)
    }

    override fun toLightFace(): LightFace {
        return this
    }

    fun put(buf: ByteBuffer) {
        buf.putFloat(vertex1.x).putFloat(vertex1.y).putFloat(vertex1.z).putFloat(0f)
        buf.putFloat(vertex2.x).putFloat(vertex2.y).putFloat(vertex2.z).putFloat(0f)
        buf.putFloat(vertex3.x).putFloat(vertex3.y).putFloat(vertex3.z).putFloat(0f)
        buf.putFloat(vertex4.x).putFloat(vertex4.y).putFloat(vertex4.z).putFloat(0f)

        buf.putFloat(texCoord1.x).putFloat(texCoord1.y)
        buf.putFloat(texCoord2.x).putFloat(texCoord2.y)
        buf.putFloat(texCoord3.x).putFloat(texCoord3.y)
        buf.putFloat(texCoord4.x).putFloat(texCoord4.y)

        buf.putFloat(normal.x).putFloat(normal.y).putFloat(normal.z).putFloat(dot)
        buf.putFloat(diagonal1.x).putFloat(diagonal1.y).putFloat(diagonal1.z).putFloat(diagonal1.dot(diagonal1))
        buf.putFloat(diagonal2.x).putFloat(diagonal2.y).putFloat(diagonal2.z).putFloat(diagonal2.dot(diagonal2))

        buf.putFloat(inverse!!.x).putFloat(inverse!!.y).putFloat(inverse!!.z).putFloat(inverse!!.w)
    }

    fun toVolumePoint(origin: Vector3f?, radius: Float): ShadowVolume {
        val d0 = normal.dot(vertex1.sub(origin, Vector3f()))
        val t = radius - d0

        val vertices = arrayOf(vertex1, vertex2, vertex3, vertex4, null, null, null, null)

        for (i in 0..3) {
            val vertex = Vector3f(vertices[i])
            val off = vertex.sub(origin, Vector3f())
            vertices[i + 4] = vertex.add(off.normalize(t))
        }

        return ShadowVolume(
            this,
            vertices
        )
    }

    fun toVolumeOrthographic(direction: Vector3f, distance: Float): ShadowVolume {
        val add = direction.mul(-distance, Vector3f())
        val vertices = arrayOf(vertex1, vertex2, vertex3, vertex4, null, null, null, null)

        for (i in 0..3) {
            vertices[i + 4] = Vector3f(vertices[i]).add(add)
        }

        return ShadowVolume(
            this,
            vertices
        )
    }

    companion object {
        const val BYTES: Int = 40 * Float.SIZE_BYTES

        fun BakedQuad.toLightFace(x: Float, y: Float, z: Float, origin: BlockPos, direction: Direction?): LightFace {
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
                direction,
                vertices[0]!!,
                vertices[1]!!,
                vertices[2]!!,
                vertices[3]!!,
                texCoords[0]!!,
                texCoords[1]!!,
                texCoords[2]!!,
                texCoords[3]!!
            )
        }
    }
}