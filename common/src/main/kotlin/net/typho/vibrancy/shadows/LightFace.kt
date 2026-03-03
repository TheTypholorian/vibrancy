package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.opengl.util.TexturedQuad
import org.joml.Vector2f
import org.joml.Vector3f
import java.util.*

@JvmRecord
data class LightFace(
    val blockPos: BlockPos?,
    val quad: TexturedQuad,
    val width: Int, val height: Int
) {
    fun buildGeometry(consumer: NeoVertexConsumer, offset: Vector3f = Vector3f()) {
        consumer.vertex(quad.v1.add(offset, Vector3f())).textureUV(quad.uv1)
        consumer.vertex(quad.v2.add(offset, Vector3f())).textureUV(quad.uv2)
        consumer.vertex(quad.v3.add(offset, Vector3f())).textureUV(quad.uv3)
        consumer.vertex(quad.v4.add(offset, Vector3f())).textureUV(quad.uv4)
    }

    open class Consumer(
        @JvmField
        val pos: BlockPos?
    ) : NeoVertexConsumer {
        @JvmField
        protected val faces = LinkedList<LightFace>()
        @JvmField
        protected var currentQuad: TexturedQuad? = null
        @JvmField
        protected var vertex: Int = 0

        protected fun start(): TexturedQuad {
            if (currentQuad == null) {
                val quad = TexturedQuad(
                    Vector3f(),
                    Vector3f(),
                    Vector3f(),
                    Vector3f(),
                    Vector2f(),
                    Vector2f(),
                    Vector2f(),
                    Vector2f()
                )
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

            when (vertex) {
                0 -> quad.v1.set(x, y, z).add(pos?.let { Vector3f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) })
                1 -> quad.v2.set(x, y, z).add(pos?.let { Vector3f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) })
                2 -> quad.v3.set(x, y, z).add(pos?.let { Vector3f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) })
                3 -> quad.v4.set(x, y, z).add(pos?.let { Vector3f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) })
            }

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

            when (vertex) {
                0 -> quad.uv1.set(u, v)
                1 -> quad.uv2.set(u, v)
                2 -> quad.uv3.set(u, v)
                3 -> quad.uv4.set(u, v)
            }

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
}