package net.typho.vibrancy

import net.typho.big_shot_lib.api.math.IRect2
import net.typho.big_shot_lib.api.math.IVec2
import kotlin.math.ceil
import kotlin.math.sqrt

object TextureAtlas {
    @JvmRecord
    data class Result(
        @JvmField
        val textures: List<IRect2<Int>>,
        @JvmField
        val size: IVec2<Int>
    )

    @JvmStatic
    fun pack(textures: List<IVec2<Int>>): Result {
        val max: IVec2<Int> = textures.fold(null) { accum, texture -> accum?.max(texture) ?: texture }
            ?: return Result(listOf(), IVec2(0, 0))

        val numSectionsX = ceil(sqrt(textures.size.toFloat())).toInt()
        val numSectionsY = ceil(textures.size.toFloat() / numSectionsX).toInt()

        val width = numSectionsX * max.x
        val height = numSectionsY * max.y

        var x = 0
        var y = 0

        return Result(
            textures.map { dimension ->
                val rect = IRect2.size(x, y, dimension.x, dimension.y)

                x += max.x

                if (x == width) {
                    x = 0
                    y += max.y
                }

                return@map rect
            },
            IVec2(width, height)
        )
    }
}