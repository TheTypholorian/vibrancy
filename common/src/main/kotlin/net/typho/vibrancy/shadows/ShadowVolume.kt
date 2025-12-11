package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.BlockPos
import net.typho.vibrancy.Vibrancy
import org.joml.Vector3f

data class ShadowVolume(
    val caster: LightFace,
    val vertices: Array<Vector3f?>
) : LightFaceConvertible {
    override fun toLightFace(): LightFace {
        return caster
    }

    fun numQuads(): Int = 6

    fun buildGeometry(consumer: VertexConsumer) {
        var i = 0
        var j = 0

        while (i < numQuads()) {
            val order = arrayOf(
                vertices[INDICES[j]]!!,
                vertices[INDICES[j + 1]]!!,
                vertices[INDICES[j + 2]]!!,
                vertices[INDICES[j + 3]]!!
            )

            for (vec in order) {
                consumer.addVertex(vec.x, vec.y, vec.z)
            }

            i++
            j += 4
        }
    }

    fun buildDebug(lightPos: BlockPos, consumer: VertexConsumer) {
        var i = 0
        var j = 0

        while (i < numQuads()) {
                val color = if (caster.direction == null || caster.blockPos == null || Vibrancy.pointsToward(
                        caster.direction,
                        Vector3f(
                            lightPos.x.toFloat() - caster.blockPos!!.x,
                            lightPos.y.toFloat() - caster.blockPos!!.y,
                            lightPos.z.toFloat() - caster.blockPos!!.z
                        )
                    )
                ) Vector3f(0f, 1f, 0f) else Vector3f(1f, 0f, 0f)

                if (i == 0) {
                    color.z = 1f
                }

                val order = arrayOf(
                    //vertices[INDICES[j]]!!,
                    //vertices[INDICES[j + 1]]!!,
                    //vertices[INDICES[j + 2]]!!,
                    //vertices[INDICES[j + 3]]!!
                    vertices[INDICES[j]]!!, vertices[INDICES[j + 1]]!!,
                    vertices[INDICES[j + 1]]!!, vertices[INDICES[j + 2]]!!,
                    vertices[INDICES[j + 2]]!!, vertices[INDICES[j + 3]]!!,
                    vertices[INDICES[j + 3]]!!, vertices[INDICES[j]]!!,
                    vertices[INDICES[j]]!!, vertices[INDICES[j + 2]]!!,
                    vertices[INDICES[j + 1]]!!, vertices[INDICES[j + 3]]!!
                )

                for (vec in order) {
                    consumer.addVertex(vec.x, vec.y, vec.z)
                        .setColor(color.x, color.y, color.z, 1f)
                }

            i++
            j += 4
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