package net.typho.vibrancy.shadows

import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoVertexData
import net.typho.big_shot_lib.api.math.rect.AbstractRect2
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec4
import net.typho.big_shot_lib.api.util.NeoColor
import kotlin.math.abs
import kotlin.math.ceil

@JvmRecord
data class LightFace(
    @JvmField
    val blockPos: IVec3<Int>?,
    @JvmField
    val state: BlockState?,
    @JvmField
    val quad: NeoBakedQuad,
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) {
    constructor(
        blockPos: IVec3<Int>?,
        state: BlockState?,
        quad: NeoBakedQuad,
        atlas: NeoAtlas
    ) : this(
        blockPos,
        state,
        quad,
        ceil(abs(quad.vertices[0].textureUV!!.y - quad.vertices[2].textureUV!!.y) * atlas.height).toInt(),
        ceil(abs(quad.vertices[0].textureUV!!.x - quad.vertices[2].textureUV!!.x) * atlas.width).toInt()
    )

    fun applyOverlay(sprite: AbstractRect2<Int>): NeoBakedQuad {
        return quad.withVertices { index, vertex ->
            NeoVertexData(
                vertex,
                overlayUV = when (index) {
                    0 -> sprite.min
                    1 -> sprite.maxMin
                    2 -> sprite.max
                    else -> sprite.minMax
                }
            )
        }
    }

    fun applyTint(tint: (pos: IVec3<Float>, color: IVec4<Float>) -> NeoColor): NeoBakedQuad {
        return quad.withVertices { index, vertex ->
            NeoVertexData(
                vertex,
                color = tint(vertex.pos, (vertex.color ?: NeoColor.FULL_ON).toVec4F())
            )
        }
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