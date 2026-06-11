package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.shadows.LightFace
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.function.Consumer

class SectionMeshCache(
    @JvmField
    var pos: SectionPos
) {
    companion object {
        @JvmStatic
        @get:JvmName("getPool")
        val POOL = ConcurrentLinkedDeque<SectionMeshCache>()

        @JvmStatic
        fun poll(pos: SectionPos): SectionMeshCache {
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
    val models = arrayOfNulls<Block?>(16 * 16 * 16)

    fun index(x: Int, y: Int, z: Int): Int = (x and 0xF shl 8) or (y and 0xF shl 4) or (z and 0xF)

    fun get(x: Int, y: Int, z: Int) = models[index(x, y, z)]

    operator fun get(pos: IVec3<Int>) = get(pos.x, pos.y, pos.z)

    operator fun get(pos: BlockPos) = get(pos.x, pos.y, pos.z)

    fun getOrCreate(x: Int, y: Int, z: Int): Block {
        val index = index(x, y, z)
        val value = models[index]

        if (value == null) {
            val block = Block()
            models[index] = block
            return block
        } else {
            return value
        }
    }

    fun getOrCreate(pos: IVec3<Int>) = getOrCreate(pos.x, pos.y, pos.z)

    fun getOrCreate(pos: BlockPos) = getOrCreate(pos.x, pos.y, pos.z)

    fun clear() {
        for (block in models) {
            if (block != null) {
                block.solidFaces?.clear()
                block.translucentFaces?.clear()
            }
        }
    }

    fun createVertexConsumer(pos: BlockPos, material: Material, atlas: NeoAtlas, offsetX: Float = 0f, offsetY: Float = 0f, offsetZ: Float = 0f): LightFace.Consumer {
        return LightFace.Consumer(
            getOrCreate(pos)[material]::add,
            atlas,
            offsetX,
            offsetY,
            offsetZ
        )
    }

    data class Block(
        @JvmField
        var solidFaces: MutableList<LightFace>? = null,
        @JvmField
        var translucentFaces: MutableList<LightFace>? = null
    ) {
        operator fun get(material: Material): MutableList<LightFace> {
            if (material.isTranslucent) {
                translucentFaces?.let { return it }

                val list = arrayListOf<LightFace>()
                translucentFaces = list
                return list
            } else {
                solidFaces?.let { return it }

                val list = arrayListOf<LightFace>()
                solidFaces = list
                return list
            }
        }

        fun collect(out: Consumer<LightFace>) {
            fun collectFrom(from: MutableList<LightFace>) {
                for (face in from) {
                    out.accept(face)
                }
            }

            solidFaces?.let { collectFrom(it) } // TODO split solid and translucent
            translucentFaces?.let { collectFrom(it) }
        }

        fun collect(consumer: BlockMeshCollector.Consumer, section: SectionPos, block: BlockPos, transmute: (face: LightFace) -> LightFace = { it }) {
            solidFaces?.let { consumer.collect(it.map(transmute), section, block, false) }
            translucentFaces?.let { consumer.collect(it.map(transmute), section, block, true) }
        }

        fun collectToList(transmute: (face: LightFace) -> LightFace = { it }): List<LightFace> {
            val list = arrayListOf<LightFace>()
            collect { list.add(transmute(it)) }
            return list
        }
    }

    interface Holder {
        var `vibrancy$sectionMeshCache`: SectionMeshCache?
    }

    interface ConsumerExtension {
        var `vibrancy$sectionMeshConsumer`: LightFace.Consumer?
    }
}