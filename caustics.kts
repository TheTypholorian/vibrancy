
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

class CellularNoise(
    @JvmField
    val seed: Int,
    @JvmField
    val cellsX: Int,
    @JvmField
    val cellsY: Int,
    @JvmField
    val cellsZ: Int
) {
    fun sample(x: Float, y: Float, z: Float): Float {
        val px = x * cellsX
        val py = y * cellsY
        val pz = z * cellsZ

        val ix = floor(px).toInt()
        val iy = floor(py).toInt()
        val iz = floor(pz).toInt()

        var minDist = Float.MAX_VALUE

        for (dz in -1..1) {
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val cx = wrap(ix + dx, cellsX)
                    val cy = wrap(iy + dy, cellsY)
                    val cz = wrap(iz + dz, cellsZ)

                    val fx = cx + random(cx, cy, cz, 0)
                    val fy = cy + random(cx, cy, cz, 1)
                    val fz = cz + random(cx, cy, cz, 2)

                    val dxp = periodic(px - fx, cellsX)
                    val dyp = periodic(py - fy, cellsY)
                    val dzp = periodic(pz - fz, cellsZ)

                    val dist = dxp * dxp + dyp * dyp + dzp * dzp

                    if (dist < minDist) {
                        minDist = dist
                    }
                }
            }
        }

        return sqrt(minDist)
    }

    fun random(x: Int, y: Int, z: Int, offset: Int): Float {
        var hash = seed
        hash = hash * 374761393 + x * 668265263
        hash = hash * 374761393 + y * 2246822519L.toInt()
        hash = hash * 374761393 + z * 3266489917L.toInt()
        hash = hash * 374761393 + offset * 1274126177

        hash = (hash xor (hash ushr 13)) * 1274126177
        hash = hash xor (hash ushr 16)

        return (hash.toLong() and 0xFFFFFFFFL).toFloat() / 4294967296.0f
    }

    fun wrap(value: Int, period: Int): Int {
        return ((value % period) + period) % period
    }

    fun periodic(value: Float, period: Int): Float {
        val half = period * 0.5f
        return value - floor((value + half) / period) * period
    }
}

val cells = 4
val minAlpha = 0.1f
val maxAlpha = 1f
val power = 3
val snap = 5

val smallSize = 16
val bigSize = 32
val tiles = 32
val smallImage = BufferedImage(smallSize, smallSize * tiles, BufferedImage.TYPE_INT_ARGB)
val bigImage = BufferedImage(bigSize, bigSize * tiles, BufferedImage.TYPE_INT_ARGB)
val noise = CellularNoise(seed = 0, cellsX = cells, cellsY = cells, cellsZ = (cells * (tiles.toFloat() / bigSize)).toInt())

fun sample(x: Float, y: Float, z: Float): Int {
    val noise = noise.sample(x, y, z).coerceIn(0f..1f).pow(power)
    val alpha = noise * (maxAlpha - minAlpha) + minAlpha
    val rounded = round(alpha * snap) / snap
    return Color(1f, 1f, 1f, rounded).rgb
}

repeat(tiles) { tile ->
    repeat(smallSize) { x ->
        repeat(smallSize) { y ->
            val nx = x.toFloat() / smallSize
            val ny = y.toFloat() / smallSize
            val nz = tile.toFloat() / tiles

            smallImage.setRGB(x, y + tile * smallSize, sample(nx, ny, nz))
        }
    }
}

repeat(tiles) { tile ->
    repeat(bigSize) { x ->
        repeat(bigSize) { y ->
            val nx = ((x.toFloat() / bigSize * 2) - 0.5f) % 1
            val ny = ((y.toFloat() / bigSize * 2) - 0.5f) % 1
            val nz = ((tile.toFloat() / tiles * 2) - 0.5f) % 1

            bigImage.setRGB(x, y + tile * bigSize, sample(nx, ny, nz))
        }
    }
}

ImageIO.write(smallImage, "png", File("src/main/resources/assets/minecraft/textures/rtx/transmission/block/water_still.png"))
ImageIO.write(bigImage, "png", File("src/main/resources/assets/minecraft/textures/rtx/transmission/block/water_flow.png"))