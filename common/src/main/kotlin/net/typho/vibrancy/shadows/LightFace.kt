package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.util.quads.NeoBakedQuad
import net.typho.big_shot_lib.api.util.IColor

@JvmRecord
data class LightFace(
    @JvmField
    val blockPos: BlockPos,
    @JvmField
    val quad: NeoBakedQuad,
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) {
    fun buildGeometry(consumer: NeoVertexConsumer, level: Level?) {
        val tintColor = level?.let {
            IColor.RGB(Minecraft.getInstance().blockColors.getColor(it.getBlockState(blockPos), level, blockPos, quad.tintIndex ?: -1))
        } ?: IColor.FULL_ON

        quad.withVertices { index, vertex -> vertex.withColor { tintColor } }.put(consumer)
    }

    /*
    open class Consumer(
        @JvmField
        val pos: BlockPos
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