package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.typho.big_shot_lib.api.client.rendering.common.GpuTexture
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.collectors.BlockMeshCollector
import java.util.BitSet
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
    @JvmField
    val stateFlags = BitSet(16 * 16 * 16 * 2)

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

    fun createVertexConsumer(pos: BlockPos, material: Material, atlas: GpuTexture, offsetX: Float = 0f, offsetY: Float = 0f, offsetZ: Float = 0f): BlockFace.Consumer {
        return BlockFace.Consumer(
            getOrCreate(pos)[material]::add,
            atlas,
            offsetX,
            offsetY,
            offsetZ
        )
    }

    data class Block(
        @JvmField
        var solidFaces: MutableList<BlockFace>? = null,
        @JvmField
        var translucentFaces: MutableList<BlockFace>? = null
    ) {
        operator fun get(material: Material): MutableList<BlockFace> {
            if (material.isTranslucent) {
                translucentFaces?.let { return it }

                val list = arrayListOf<BlockFace>()
                translucentFaces = list
                return list
            } else {
                solidFaces?.let { return it }

                val list = arrayListOf<BlockFace>()
                solidFaces = list
                return list
            }
        }

        fun collect(out: Consumer<BlockFace>) {
            fun collectFrom(from: MutableList<BlockFace>) {
                for (face in from) {
                    out.accept(face)
                }
            }

            solidFaces?.let { collectFrom(it) } // TODO split solid and translucent
            translucentFaces?.let { collectFrom(it) }
        }

        fun collect(consumer: BlockMeshCollector.Consumer, section: SectionPos, block: BlockPos, transmute: (face: BlockFace) -> BlockFace = { it }) {
            solidFaces?.let { consumer.collect(it.map(transmute), section, block, false) }
            translucentFaces?.let { consumer.collect(it.map(transmute), section, block, true) }
        }

        fun collectToList(transmute: (face: BlockFace) -> BlockFace = { it }): List<BlockFace> {
            val list = arrayListOf<BlockFace>()
            collect { list.add(transmute(it)) }
            return list
        }
    }

    interface Holder {
        var `vibrancy$sectionMeshCache`: SectionMeshCache?
    }

    interface ConsumerExtension {
        var `vibrancy$sectionMeshConsumer`: BlockFace.Consumer?
    }
}