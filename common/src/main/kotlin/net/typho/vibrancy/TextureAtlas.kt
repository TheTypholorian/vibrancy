package net.typho.vibrancy

import net.typho.big_shot_lib.api.client.opengl.buffers.GlBuffer
import org.lwjgl.system.MemoryStack
import java.awt.Dimension
import java.awt.Rectangle
import java.util.*

object TextureAtlas {
    private fun Dimension.max(other: Dimension): Dimension {
        return Dimension(
            width.coerceAtLeast(other.width),
            height.coerceAtLeast(other.height)
        )
    }

    @JvmStatic
    fun pack(vararg textures: Dimension, buffer: GlBuffer) {
        MemoryStack.stackPush().use { stack ->
            val array = pack(*textures)
            buffer.upload(
                array.fold(stack.mallocInt(array.size * 4)) { buffer, texture ->
                    buffer.put(texture.x)
                        .put(texture.y)
                        .put(texture.width)
                        .put(texture.height)
                }
            )
        }
    }

    @JvmStatic
    fun pack(vararg textures: Dimension): Array<Rectangle> {
        val max: Dimension = textures.fold(null) { accum, texture -> accum?.max(texture) ?: texture }
            ?: return arrayOf()

        data class Section(
            @JvmField
            val offsetX: Int,
            @JvmField
            val textures: MutableList<Pair<Int, Rectangle>> = LinkedList()
        ) {
            fun trimWidth(): Int {
                return this.textures.maxOfOrNull { it.second.x + it.second.width } ?: 0
            }

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
                sections.add(Section(sections.sumOf { it.trimWidth() }, mutableListOf(texture.first to Rectangle(texture.second))))
            } else {
                val last = sections.last()

                if (last.fit(texture.first, texture.second) == null) {
                    Section(sections.sumOf { it.trimWidth() }).also(sections::add)
                        .fit(texture.first, texture.second)
                        ?: throw IllegalStateException()
                }
            }
        }

        val array = arrayOfNulls<Rectangle>(textures.size)
        sections.forEach { section ->
            section.textures.forEach {
                array[it.first] = Rectangle(it.second.x + section.offsetX, it.second.y, it.second.width, it.second.height)
            }
        }
        return array.map { it!! }.toTypedArray()
    }
}