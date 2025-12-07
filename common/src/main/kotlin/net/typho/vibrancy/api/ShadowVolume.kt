package net.typho.vibrancy.api

import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Vector3f

@JvmRecord
data class ShadowVolume(val caster: LightFace, val vertices: Array<Vector3f?>) : LightFaceConvertible {
    override fun toLightFace(): LightFace {
        return caster
    }

    fun buildGeometry(consumer: VertexConsumer) {
        for (index in INDICES) {
            consumer.addVertex(vertices[index]!!)
        }
    }

    companion object {
        val INDICES: IntArray = intArrayOf(
            0, 1, 2, 3,
            1, 5, 6, 2,
            5, 4, 7, 6,
            4, 0, 3, 7,
            1, 0, 4, 5,
            3, 2, 6, 7
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShadowVolume

        if (caster != other.caster) return false
        if (!vertices.contentEquals(other.vertices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = caster.hashCode()
        result = 31 * result + vertices.contentHashCode()
        return result
    }
}