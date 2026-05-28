package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoVertexData
import net.typho.big_shot_lib.api.math.rect.AbstractRect2
import net.typho.big_shot_lib.api.math.vec.IVec2
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.util.EmptyVertexConsumer.quad
import kotlin.math.abs
import kotlin.math.ceil

@JvmRecord
data class LightFace(
    @JvmField
    val v0: Vertex,
    @JvmField
    val v1: Vertex,
    @JvmField
    val v2: Vertex,
    @JvmField
    val v3: Vertex,
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) {
    constructor(
        v0: Vertex,
        v1: Vertex,
        v2: Vertex,
        v3: Vertex,
        atlas: NeoAtlas
    ) : this(
        v0,
        v1,
        v2,
        v3,
        ceil(abs(v0.v - v2.v) * atlas.height).toInt(),
        ceil(abs(v0.u - v2.u) * atlas.width).toInt()
    )

    class Vertex(
        @JvmField
        var x: Float,
        @JvmField
        var y: Float,
        @JvmField
        var z: Float,
        @JvmField
        var color: Int,
        @JvmField
        var u: Float,
        @JvmField
        var v: Float,
        @JvmField
        var light: Int,
        @JvmField
        var normal: Int
    ) {
        constructor() : this(0f, 0f, 0f, 0, 0f, 0f, 0, 0)
    }

    fun copyWithOffset(x: Float, y: Float, z: Float): LightFace {
        return copy(
            v0 = Vertex(
                v0.x + x,
                v0.y + y,
                v0.z + z,
                v0.color,
                v0.u,
                v0.v,
                v0.light,
                v0.normal
            ),
            v1 = Vertex(
                v1.x + x,
                v1.y + y,
                v1.z + z,
                v1.color,
                v1.u,
                v1.v,
                v1.light,
                v1.normal
            ),
            v2 = Vertex(
                v2.x + x,
                v2.y + y,
                v2.z + z,
                v2.color,
                v2.u,
                v2.v,
                v2.light,
                v2.normal
            ),
            v3 = Vertex(
                v3.x + x,
                v3.y + y,
                v3.z + z,
                v3.color,
                v3.u,
                v3.v,
                v3.light,
                v3.normal
            )
        )
    }

    fun apply(out: (vertex: Vertex, index: Int) -> Unit) {
        out(v0, 0)
        out(v1, 1)
        out(v2, 2)
        out(v3, 3)
    }

    fun apply(out: (vertex: Vertex) -> Unit) {
        out(v0)
        out(v1)
        out(v2)
        out(v3)
    }

    /*
    open class Consumer(
        @JvmField
        val pos: IVec3<Int>
    ) : NeoVertexConsumer {
        @JvmField
        protected val faces = LinkedList<LightFace>()
        @JvmField
        protected var currentQuad: Array<NeoVertexData>? = null
        @JvmField
        protected var vertex: Int = 0

        protected fun start(): Array<NeoVertexData> {
            if (currentQuad == null) {
                val quad = Array<NeoVertexData>(4) {
                    BasicVertexData(
                        Vector3f(),
                        null,
                        Vector2f(),
                        null,
                        null,
                        null
                    )
                }
                currentQuad = quad
                return quad
            } else {
                if (vertex < 4) {
                    return currentQuad!!
                }

                faces.add(LightFace(pos, currentQuad!!, 1, 1))
                currentQuad = null
                return start()
            }
        }

        fun end(): List<LightFace> {
            start()
            return faces
        }

        override fun vertex(
            x: Float,
            y: Float,
            z: Float
        ): NeoVertexConsumer {
            val quad = start()

            quad[vertex] = quad[vertex].withPosition { Vector3f(x, y, z) }

            return this
        }

        override fun color(
            r: Float,
            g: Float,
            b: Float,
            a: Float
        ): NeoVertexConsumer {
            return this
        }

        override fun textureUV(
            u: Float,
            v: Float
        ): NeoVertexConsumer {
            val quad = start()
            val vertex = vertex++
            quad[vertex] = quad[vertex].withTextureUV { Vector2f(u, v) }
            return this
        }

        override fun overlayUV(
            u: Int,
            v: Int
        ): NeoVertexConsumer {
            return this
        }

        override fun lightUV(
            u: Int,
            v: Int
        ): NeoVertexConsumer {
            return this
        }

        override fun normal(
            x: Float,
            y: Float,
            z: Float
        ): NeoVertexConsumer {
            return this
        }
    }
     */
}