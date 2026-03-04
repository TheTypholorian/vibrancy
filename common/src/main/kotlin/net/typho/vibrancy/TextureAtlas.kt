package net.typho.vibrancy

import net.typho.big_shot_lib.api.client.opengl.buffers.GlBuffer
import org.lwjgl.system.MemoryUtil
import java.awt.Dimension
import java.awt.Rectangle
import java.util.*
import kotlin.math.ceil
import kotlin.math.sqrt

object TextureAtlas {
    @JvmStatic
    fun main(args: Array<String>) {
        println(pack(
            Dimension(16, 16),
            Dimension(3, 16),
            Dimension(3, 8),
            Dimension(2, 8),
            Dimension(16, 8),
            Dimension(16, 4),
            Dimension(16, 4),
        ))
        println(Rectangle(0, 0, 16, 16).intersects(Rectangle(0, 17, 16, 16)))
        println(Rectangle(0, 0, 16, 16).intersects(Rectangle(0, 16, 16, 16)))
        println(Rectangle(0, 0, 16, 16).intersects(Rectangle(0, 15, 16, 16)))
    }

    private fun Dimension.max(other: Dimension): Dimension {
        return Dimension(
            width.coerceAtLeast(other.width),
            height.coerceAtLeast(other.height)
        )
    }

    @JvmRecord
    data class Result(
        @JvmField
        val textures: Array<Rectangle>,
        @JvmField
        val width: Int,
        @JvmField
        val height: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Result

            if (width != other.width) return false
            if (height != other.height) return false
            if (!textures.contentEquals(other.textures)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = width
            result = 31 * result + height
            result = 31 * result + textures.contentHashCode()
            return result
        }
    }

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
            ?: return Result(arrayOf(), 0, 0)

        data class Section(
            @JvmField
            val textures: MutableList<Pair<Int, Rectangle>> = LinkedList()
        ) {
            fun fit(id: Int, texture: Dimension): Rectangle? {
                var x = 0

                while (x <= max.width - texture.width) {
                    var minShift: Int? = null

                    repeat(max.height - texture.height + 1) { y ->
                        val candidate = Rectangle(x, y, texture.width, texture.height)

                        for (bound in this.textures) {
                            val overlap = bound.second.x + bound.second.width - x

                            if (overlap > 0) {
                                minShift = minShift?.coerceAtMost(overlap) ?: overlap
                            }

                            if (bound.second.intersects(candidate)) {
                                return@repeat
                            }
                        }

                        this.textures.add(id to candidate)
                        return candidate
                    }

                    x += (minShift ?: 0).coerceAtLeast(1)
                }

                return null
            }
        }

        val pool = textures.mapIndexed { index, dimension -> index to dimension }
            .sortedWith(Comparator.comparingInt { it.second.width * it.second.height })
            .toMutableList()
        val sections = LinkedList<Section>()

        while (pool.isNotEmpty()) {
            val texture = pool.removeLast()

            if (texture.second.width == max.width && texture.second.height == max.height) {
                sections.add(Section(mutableListOf(texture.first to Rectangle(texture.second))))
            } else {
                var last = sections.lastOrNull()

                if (last == null) {
                    last = Section().also(sections::add)
                }

                if (last.fit(texture.first, texture.second) == null) {
                    Section().also(sections::add)
                        .fit(texture.first, texture.second)
                        ?: throw IllegalStateException()
                }
            }
        }

        val array = arrayOfNulls<Rectangle>(textures.size)

        val numSectionsX = ceil(sqrt(sections.size.toFloat())).toInt()
        val numSectionsY = ceil(sections.size.toFloat() / numSectionsX).toInt()
        var index = 0

        repeat(numSectionsX) { x ->
            repeat(numSectionsY) { y ->
                val i = index++

                if (i < sections.size) {
                    val section = sections[i]

                    section.textures.forEach {
                        array[it.first] = Rectangle(it.second.x + x * max.width, it.second.y + y * max.height, it.second.width, it.second.height)
                    }
                }
            }
        }

        for (a in array) {
            for (b in array) {
                if (a !== b) {
                    if (a!!.intersects(b!!)) {
                        throw IllegalStateException("$a $b")
                    }
                }
            }
        }

        return Result(array.map { it!! }.toTypedArray(), numSectionsX * max.width, numSectionsY * max.height)
    }
}