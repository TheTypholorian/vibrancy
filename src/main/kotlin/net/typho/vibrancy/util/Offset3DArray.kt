package net.typho.vibrancy.util

import net.minecraft.core.Vec3i
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import org.joml.Vector3i
import java.util.Arrays

@Suppress("UNCHECKED_CAST")
open class Offset3DArray<T>(
    @JvmField
    val bounds: AbstractRect3<Int>,
    @JvmField
    val initializer: RecursiveInitializer<T>
) : Iterable<Pair<IVec3<Int>, T>> {
    protected val array = Array(bounds.size.x + 1) { x ->
        val xInit = initializer(x)
        Array(bounds.size.y + 1) { y ->
            Array<Any?>(bounds.size.z + 1, xInit(y))
        }
    }

    constructor(
        bounds: AbstractRect3<Int>,
        value: T
    ) : this(bounds, { x -> { y -> { z -> value } } })

    fun isInBounds(x: Int, y: Int, z: Int) = bounds.min.allLequalThan(x, y, z) && bounds.max.allGequalThan(x, y, z) // TODO

    fun isInBounds(pos: IVec3<Int>) = isInBounds(pos.x, pos.y, pos.z)

    fun isInBounds(pos: Vec3i) = isInBounds(pos.x, pos.y, pos.z)

    fun isInBounds(pos: Vector3i) = isInBounds(pos.x, pos.y, pos.z)

    fun get(x: Int, y: Int, z: Int): T {
        return array[x - bounds.min.x][y - bounds.min.y][z - bounds.min.z] as T
    }

    operator fun get(pos: IVec3<Int>) = get(pos.x, pos.y, pos.z)

    operator fun get(pos: Vec3i) = get(pos.x, pos.y, pos.z)

    operator fun get(pos: Vector3i) = get(pos.x, pos.y, pos.z)

    fun set(x: Int, y: Int, z: Int, value: T) {
        array[x - bounds.min.x][y - bounds.min.y][z - bounds.min.z] = value
    }

    operator fun set(pos: IVec3<Int>, value: T) = set(pos.x, pos.y, pos.z, value)

    operator fun set(pos: Vec3i, value: T) = set(pos.x, pos.y, pos.z, value)

    operator fun set(pos: Vector3i, value: T) = set(pos.x, pos.y, pos.z, value)

    fun getAndSet(x: Int, y: Int, z: Int, value: T): T {
        val a = array[x - bounds.min.x][y - bounds.min.y]
        val old = a[z - bounds.min.z] as T
        a[z - bounds.min.z] = value
        return old
    }

    fun getAndSet(pos: IVec3<Int>, value: T) = getAndSet(pos.x, pos.y, pos.z, value)

    fun getAndSet(pos: Vec3i, value: T) = getAndSet(pos.x, pos.y, pos.z, value)

    fun getAndSet(pos: Vector3i, value: T) = getAndSet(pos.x, pos.y, pos.z, value)

    fun fill(mapper: RecursiveMapper<T, T>) {
        array.forEachIndexed { x, xa ->
            val xInit = mapper(x)
            xa.forEachIndexed { y, ya ->
                val yInit = xInit(y)
                ya.forEachIndexed { z, old ->
                    ya[z] = yInit(z, old as T)
                }
            }
        }
    }

    fun fill(value: T) {
        for (xa in array) {
            for (ya in xa) {
                Arrays.fill(ya, value)
            }
        }
    }

    override fun iterator(): Iterator<Pair<IVec3<Int>, T>> {
        return object : Iterator<Pair<IVec3<Int>, T>> {
            var x = 0
            var y = 0
            var z = 0
            var xArray = array[x]
            var yArray = xArray[y]

            override fun hasNext(): Boolean {
                return x < bounds.size.x
            }

            override fun next(): Pair<IVec3<Int>, T> {
                val item = yArray[z] as T
                val pos = NeoVec3i(x, y, z)

                z++

                if (z >= yArray.size) {
                    z = 0
                    y++

                    if (y >= xArray.size) {
                        y = 0
                        x++
                        xArray = array[x]
                        yArray = xArray[y]
                    } else {
                        yArray = xArray[y]
                    }
                }

                return pos to item
            }
        }
    }

    fun interface RecursiveInitializer<T> {
        operator fun invoke(x: Int): (y: Int) -> (z: Int) -> T
    }

    fun interface RecursiveMapper<T, V> {
        operator fun invoke(x: Int): (y: Int) -> (z: Int, old: T) -> V
    }

    fun interface FlatInitializer<T> : RecursiveInitializer<T> {
        operator fun invoke(x: Int, y: Int, z: Int): T

        override fun invoke(x: Int): (y: Int) -> (z: Int) -> T {
            return { y -> { z -> invoke(x, y, z) } }
        }
    }

    fun interface FlatMapper<T, V> : RecursiveMapper<T, V> {
        operator fun invoke(x: Int, y: Int, z: Int, old: T): V

        override fun invoke(x: Int): (y: Int) -> (z: Int, old: T) -> V {
            return { y -> { z, old -> invoke(x, y, z, old) } }
        }
    }

    fun <V> map(mapper: RecursiveMapper<T, V>): Offset3DArray<V> {
        return Offset3DArray(bounds, RecursiveInitializer { x ->
            val xArray = array[x]
            val xInit = mapper(x)
            return@RecursiveInitializer { y ->
                val yArray = xArray[y]
                val yInit = xInit(y)
                return@RecursiveInitializer { z ->
                    yInit(z, yArray[z] as T)
                }
            }
        })
    }

    @JvmOverloads
    fun copy(mapper: RecursiveMapper<T, T> = RecursiveMapper { x -> { y -> { z, old -> old } } }): Offset3DArray<T> {
        return map(mapper)
    }
}