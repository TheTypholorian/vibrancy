package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.util.quads.NeoAtlas
import net.typho.big_shot_lib.api.client.util.quads.NeoBakedQuad
import net.typho.big_shot_lib.api.util.IColor
import org.joml.Vector2i
import org.joml.Vector3f
import java.awt.Rectangle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

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
    constructor(
        blockPos: BlockPos,
        quad: NeoBakedQuad,
        atlas: NeoAtlas
    ) : this(
        blockPos,
        quad,
        ceil(abs(quad.vertices[0].textureUV!!.y() - quad.vertices[2].textureUV!!.y()) * atlas.height).toInt(),
        ceil(abs(quad.vertices[0].textureUV!!.x() - quad.vertices[2].textureUV!!.x()) * atlas.width).toInt()
    )

    fun buildGeometry(consumer: NeoVertexConsumer, lightSprite: Rectangle?, level: Level?) {
        val tintColor = if (level != null && quad.tintIndex != null) {
            IColor.RGB(Minecraft.getInstance().blockColors.getColor(level.getBlockState(blockPos), level, blockPos, quad.tintIndex!!))
        } else {
            IColor.FULL_ON
        }

        quad.withVertices { index, vertex ->
            var vertex = vertex.withColor { tintColor }

            lightSprite?.let { sprite -> vertex = vertex.withOverlayUV { when (index) {
                0 -> Vector2i(sprite.x, sprite.y)
                1 -> Vector2i(sprite.x + sprite.width, sprite.y)
                2 -> Vector2i(sprite.x + sprite.width, sprite.y + sprite.height)
                else -> Vector2i(sprite.x, sprite.y + sprite.height)
            } } }

            vertex
        }.put(consumer)
    }

    fun split(maxSize: Int): Array<LightFace> {
        if (width <= maxSize && height <= maxSize) {
            return arrayOf(this)
        } else {
            val numX = ceil(width.toFloat() / maxSize).toInt()
            val numY = ceil(height.toFloat() / maxSize).toInt()
            val scaleX = maxSize.toFloat() / width
            val scaleY = maxSize.toFloat() / height
            return Array(numX * numY) { index ->
                val x = index % numX
                val y = (index - x) / numY

                val minX = scaleX * x
                val minY = scaleY * y
                val maxX = min(1f, scaleX * (x + 1))
                val maxY = min(1f, scaleY * (y + 1))

                return@Array LightFace(
                    blockPos,
                    quad.withVertices { index, vertex ->
                        val fx = when (index) {
                            0, 3 -> minX
                            else -> maxX
                        }
                        val fy = when (index) {
                            0, 1 -> minY
                            else -> maxY
                        }
                        return@withVertices vertex.withPosition {
                            quad.v0.pos.lerp(quad.v1.pos, fx, Vector3f())
                                .lerp(
                                    quad.v3.pos.lerp(quad.v2.pos, fx, Vector3f()),
                                    fy,
                                    Vector3f()
                                )
                        }
                    },
                    width.coerceAtMost(maxSize),
                    height.coerceAtMost(maxSize)
                )
            }
        }
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