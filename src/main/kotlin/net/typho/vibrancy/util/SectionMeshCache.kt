package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.api.util.ColorABGR
import net.caffeinemc.mods.sodium.api.util.ColorARGB
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexConsumer
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.shadows.LightFace
import java.util.concurrent.ConcurrentLinkedDeque

class SectionMeshCache(
    @JvmField
    var pos: SectionPos
) {
    companion object {
        @JvmStatic
        @get:JvmName("getPool")
        val POOL = ConcurrentLinkedDeque<SectionMeshCache>()

        @JvmStatic
        operator fun get(pos: SectionPos): SectionMeshCache {
            val cache = POOL.poll()

            if (cache == null) {
                return SectionMeshCache(pos)
            } else {
                cache.pos = pos
                cache.clear()
                return cache
            }
        }
    }

    @JvmField
    val models = Array(16) { Array(16) { Array(16) { Block(arrayListOf(), arrayListOf()) } } }

    fun get(x: Int, y: Int, z: Int) = models[x and 15][y and 15][z and 15]

    operator fun get(pos: IVec3<Int>) = get(pos.x, pos.y, pos.z)

    operator fun get(pos: BlockPos) = get(pos.x, pos.y, pos.z)

    fun clear() {
        for (arrays in models) {
            for (blocks in arrays) {
                for (block in blocks) {
                    block.solidFaces.clear()
                    block.translucentFaces.clear()
                }
            }
        }
    }

    fun createVertexConsumer(pos: BlockPos, translucent: Boolean, atlas: NeoAtlas, offsetX: Float = 0f, offsetY: Float = 0f, offsetZ: Float = 0f): Consumer {
        return Consumer(if (translucent) get(pos).translucentFaces else get(pos).solidFaces, atlas, offsetX, offsetY, offsetZ)
    }

    class Consumer(
        @JvmField
        val faces: MutableList<LightFace>,
        @JvmField
        val atlas: NeoAtlas,
        @JvmField
        val offsetX: Float,
        @JvmField
        val offsetY: Float,
        @JvmField
        val offsetZ: Float
    ) : NeoVertexConsumer() {
        private var v0 = LightFace.Vertex()
        private var v1 = LightFace.Vertex()
        private var v2 = LightFace.Vertex()
        private var v3 = LightFace.Vertex()
        private var vertex = v0
        private var index = 0

        fun flush() {
            if (index == 4) {
                index = 0
                faces.add(LightFace(v0, v1, v2, v3, atlas))
                v0 = LightFace.Vertex()
                v1 = LightFace.Vertex()
                v2 = LightFace.Vertex()
                v3 = LightFace.Vertex()
            }
        }

        override fun vertex(
            x: Float,
            y: Float,
            z: Float
        ): NeoVertexConsumer {
            flush()
            vertex = when (index) {
                0 -> v0
                1 -> v1
                2 -> v2
                3 -> v3
                else -> throw IndexOutOfBoundsException(index)
            }
            index++
            vertex.x = x + offsetX
            vertex.y = y + offsetY
            vertex.z = z + offsetZ
            return this
        }

        override fun color(
            r: Int,
            g: Int,
            b: Int,
            a: Int
        ): NeoVertexConsumer {
            vertex.color = ColorARGB.pack(r, g, b, a)
            return this
        }

        override fun color(argb: Int): NeoVertexConsumer {
            vertex.color = argb
            return this
        }

        override fun lightUV(
            u: Int,
            v: Int
        ): NeoVertexConsumer {
            vertex.light = (u shl 16) or v
            return this
        }

        override fun lightUV(packed: Int): NeoVertexConsumer {
            vertex.light = packed
            return this
        }

        override fun normal(
            x: Float,
            y: Float,
            z: Float
        ): NeoVertexConsumer {
            vertex.normal = ((x * 127).toInt() shl 16) or ((y * 127).toInt() shl 8) or (z * 127).toInt()
            return this
        }

        override fun normal(packed: Int): NeoVertexConsumer {
            vertex.normal = packed
            return this
        }

        override fun overlayUV(
            u: Int,
            v: Int
        ): NeoVertexConsumer {
            return this
        }

        override fun textureUV(
            u: Float,
            v: Float
        ): NeoVertexConsumer {
            vertex.u = u
            vertex.v = v
            return this
        }
    }

    data class Block(
        @JvmField
        val solidFaces: MutableList<LightFace>,
        @JvmField
        val translucentFaces: MutableList<LightFace>,
    ) {
        operator fun get(translucent: Boolean) = if (translucent) translucentFaces else solidFaces
    }

    interface Holder {
        var `vibrancy$sectionMeshCache`: SectionMeshCache?
    }

    interface ConsumerExtension {
        var `vibrancy$sectionMeshConsumer`: Consumer?
    }
}