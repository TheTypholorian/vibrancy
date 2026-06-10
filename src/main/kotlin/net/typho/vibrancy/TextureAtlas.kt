package net.typho.vibrancy

import net.typho.big_shot_lib.api.math.rect.AbstractRect2
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.vec.IVec2
import net.typho.big_shot_lib.api.math.vec.NeoVec2i
import kotlin.math.ceil
import kotlin.math.sqrt

object TextureAtlas {
    @JvmRecord
    data class Result(
        @JvmField
        val textures: List<AbstractRect2<Int>>,
        @JvmField
        val size: IVec2<Int>
    )

    @JvmStatic
    fun pack(textures: List<IVec2<Int>>): Result {
        val max: IVec2<Int> = textures.fold(null) { accum, texture -> accum?.max(texture) ?: texture }
            ?: return Result(listOf(), NeoVec2i(0, 0))

        val numSectionsX = ceil(sqrt(textures.size.toFloat())).toInt()
        val numSectionsY = ceil(textures.size.toFloat() / numSectionsX).toInt()

        val width = numSectionsX * max.x
        val height = numSectionsY * max.y

        var x = 0
        var y = 0

        return Result(
            textures.map { dimension ->
                val rect = NeoRect2i(x, y, x + dimension.x, y + dimension.y)

                x += max.x

                if (x == width) {
                    x = 0
                    y += max.y
                }

                return@map rect
            },
            NeoVec2i(width, height)
        )
    }
}