package net.typho.vibrancy

import net.typho.big_shot_lib.api.client.opengl.buffers.GlBuffer
import org.lwjgl.system.MemoryUtil
import java.awt.Dimension
import java.awt.Rectangle
import kotlin.math.ceil
import kotlin.math.sqrt

object TextureAtlas {
    private fun Dimension.max(other: Dimension): Dimension {
        return Dimension(
            width.coerceAtLeast(other.width),
            height.coerceAtLeast(other.height)
        )
    }

    @JvmRecord
    data class Result(
        @JvmField
        val textures: List<Rectangle>,
        @JvmField
        val width: Int,
        @JvmField
        val height: Int
    )

    @JvmStatic
    fun store(result: Result, buffer: GlBuffer) {
        val ints = MemoryUtil.memAllocInt(result.textures.size * 4)

        buffer.upload(
            result.textures.fold(ints) { buffer, texture ->
                buffer.put(texture.x)
                    .put(texture.y)
                    .put(texture.width)
                    .put(texture.height)
            }.flip()
        )

        MemoryUtil.memFree(ints)
    }

    @JvmStatic
    fun pack(vararg textures: Dimension): Result {
        val max: Dimension = textures.fold(null) { accum, texture -> accum?.max(texture) ?: texture }
            ?: return Result(listOf(), 0, 0)

        val numSectionsX = ceil(sqrt(textures.size.toFloat())).toInt()
        val numSectionsY = ceil(textures.size.toFloat() / numSectionsX).toInt()

        val width = numSectionsX * max.width
        val height = numSectionsY * max.height

        var x = 0
        var y = 0

        return Result(
            textures.map { dimension ->
                val rect = Rectangle(x, y, dimension.width, dimension.height)

                x += max.width

                if (x == width) {
                    x = 0
                    y += max.height
                }

                return@map rect
            },
            width,
            height
        )
    }
}