package net.typho.vibrancy.api

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.typho.vibrancy.Vibrancy
import org.joml.Vector3f

data class ShadowVolume(
    val caster: LightFace,
                        val vertices: Array<Vector3f?>,
    var directions: Array<Direction>? = null
) : LightFaceConvertible {
    override fun toLightFace(): LightFace {
        return caster
    }

    init {
        caster.direction?.let {
            val ax = caster.direction.axis
            val ax1 = when (ax) {
                Direction.Axis.X -> Direction.Axis.Y
                Direction.Axis.Y -> Direction.Axis.Z
                Direction.Axis.Z -> Direction.Axis.X
            }
            val ax2 = when (ax) {
                Direction.Axis.X -> Direction.Axis.Z
                Direction.Axis.Y -> Direction.Axis.X
                Direction.Axis.Z -> Direction.Axis.Y
            }
            val f0 = caster.direction
            val f1 = caster.direction.getClockWise(ax1)
            val f2 = caster.direction.opposite
            val f3 = caster.direction.getCounterClockWise(ax1)
            val f4 = caster.direction.getClockWise(ax2)
            val f5 = caster.direction.getCounterClockWise(ax2)
            directions = arrayOf(f0, f1, f2, f3, f4, f5)
        }
    }

    fun numQuads(): Int {
        if (directions == null) {
            return 6
        }

        var num = 0

        for (i in 0 until 6) {
            if (directions == null || (caster.mask and (1 shl directions!![i].ordinal) != 0)) {
                num++
            }
        }

        return num
    }

    fun buildGeometry(consumer: VertexConsumer): Int {
        var i = 0
        var j = 0
        var k = 0

        while (i < 6) {
            if (directions == null || (caster.mask and (1 shl directions!![i].ordinal) != 0)) {
                k++

                val order = arrayOf(
                    vertices[INDICES[j]]!!,
                    vertices[INDICES[j + 1]]!!,
                    vertices[INDICES[j + 2]]!!,
                    vertices[INDICES[j + 3]]!!
                )

                for (vec in order) {
                    consumer.addVertex(vec.x, vec.y, vec.z)
                }
            }

            i++
            j += 4
        }

        return k
    }

    fun buildDebug(lightPos: BlockPos, consumer: VertexConsumer) {
        var i = 0
        var j = 0

        while (i < 6) {
            if (directions == null || (caster.mask and (1 shl directions!![i].ordinal) != 0)) {
                val color = if (caster.direction == null || Vibrancy.pointsToward(
                        caster.direction,
                        Vector3f(
                            lightPos.x.toFloat() - caster.blockPos.x,
                            lightPos.y.toFloat() - caster.blockPos.y,
                            lightPos.z.toFloat() - caster.blockPos.z
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
            }

            i++
            j += 4
        }
    }

    companion object {
        val INDICES: IntArray = intArrayOf(
            0, 1, 2, 3, // front
            1, 5, 6, 2, // CCW top 1
            5, 4, 7, 6, // back
            4, 0, 3, 7, // CW top 1
            1, 0, 4, 5, // up
            3, 2, 6, 7 // down
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