package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.opengl.util.TexturedQuad
import net.typho.big_shot_lib.api.util.IColor
import org.joml.Vector2f
import org.joml.Vector3f
import java.util.*

@JvmRecord
data class LightFace(
    @JvmField
    val blockPos: BlockPos,
    @JvmField
    val quad: TexturedQuad,
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) {
    fun buildGeometry(consumer: NeoVertexConsumer, level: Level?) {
        val tintColor = level?.let {
            IColor.RGBA(Minecraft.getInstance().blockColors.getColor(it.getBlockState(blockPos), level, blockPos, 0))
        } ?: IColor.FULL_ON

        consumer.vertex(quad.v1).textureUV(quad.uv1).color(tintColor)
        consumer.vertex(quad.v2).textureUV(quad.uv2).color(tintColor)
        consumer.vertex(quad.v3).textureUV(quad.uv3).color(tintColor)
        consumer.vertex(quad.v4).textureUV(quad.uv4).color(tintColor)
    }

    open class Consumer(
        @JvmField
        val pos: BlockPos
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
                    Vector2f(),
                    -1
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
                0 -> quad.v1.set(x, y, z)
                1 -> quad.v2.set(x, y, z)
                2 -> quad.v3.set(x, y, z)
                3 -> quad.v4.set(x, y, z)
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