package net.typho.vibrancy.shadows

import net.caffeinemc.mods.sodium.api.util.ColorARGB
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexConsumer

@JvmRecord
data class DynamicLightFace(
    override val v0: PrimitiveVertex,
    override val v1: PrimitiveVertex,
    override val v2: PrimitiveVertex,
    override val v3: PrimitiveVertex,
) : PrimitiveQuad {
    fun copyWithOffset(x: Float, y: Float, z: Float): DynamicLightFace {
        return copy(
            v0 = PrimitiveVertex(v0, x, y, z),
            v1 = PrimitiveVertex(v1, x, y, z),
            v2 = PrimitiveVertex(v2, x, y, z),
            v3 = PrimitiveVertex(v3, x, y, z)
        )
    }

    open class Consumer(
        @JvmField
        val out: (face: DynamicLightFace) -> Unit,
        @JvmField
        val offsetX: Float = 0f,
        @JvmField
        val offsetY: Float = 0f,
        @JvmField
        val offsetZ: Float = 0f,
    ) : NeoVertexConsumer() {
        private var v0 = PrimitiveVertex()
        private var v1 = PrimitiveVertex()
        private var v2 = PrimitiveVertex()
        private var v3 = PrimitiveVertex()
        private var vertex = v0
        private var index = 0

        fun flush() {
            if (index == 4) {
                index = 0
                out(DynamicLightFace(v0, v1, v2, v3))
                v0 = PrimitiveVertex()
                v1 = PrimitiveVertex()
                v2 = PrimitiveVertex()
                v3 = PrimitiveVertex()
            }
        }

        override fun vertex(
            x: Float,
            y: Float,
            z: Float
        ): NeoVertexConsumer {
            flush()
            vertex = when (index) {
                0 -> v0
                1 -> v1
                2 -> v2
                3 -> v3
                else -> throw IndexOutOfBoundsException(index)
            }
            index++
            vertex.x = x + offsetX
            vertex.y = y + offsetY
            vertex.z = z + offsetZ
            return this
        }

        override fun color(
            r: Int,
            g: Int,
            b: Int,
            a: Int
        ): NeoVertexConsumer {
            vertex.color = ColorARGB.pack(r, g, b, a)
            return this
        }

        override fun color(argb: Int): NeoVertexConsumer {
            vertex.color = argb
            return this
        }

        override fun lightUV(
            u: Int,
            v: Int
        ): NeoVertexConsumer {
            vertex.light = (u shl 16) or v
            return this
        }

        override fun lightUV(packed: Int): NeoVertexConsumer {
            vertex.light = packed
            return this
        }

        override fun normal(
            x: Float,
            y: Float,
            z: Float
        ): NeoVertexConsumer {
            vertex.normal = ((x * 127).toInt() shl 16) or ((y * 127).toInt() shl 8) or (z * 127).toInt()
            return this
        }

        override fun normal(packed: Int): NeoVertexConsumer {
            vertex.normal = packed
            return this
        }

        override fun overlayUV(
            u: Int,
            v: Int
        ): NeoVertexConsumer {
            return this
        }

        override fun textureUV(
            u: Float,
            v: Float
        ): NeoVertexConsumer {
            vertex.u = u
            vertex.v = v
            return this
        }
    }
}