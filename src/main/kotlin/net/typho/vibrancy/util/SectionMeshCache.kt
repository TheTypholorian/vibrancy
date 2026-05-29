package net.typho.vibrancy.util

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
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
    val models = Array(16 * 16 * 16) { Block(arrayListOf(), arrayListOf()) }

    fun index(x: Int, y: Int, z: Int): Int = (x and 0xF shl 8) or (y and 0xF shl 4) or (z and 0xF)

    fun get(x: Int, y: Int, z: Int) = models[index(x, y, z)]

    operator fun get(pos: IVec3<Int>) = get(pos.x, pos.y, pos.z)

    operator fun get(pos: BlockPos) = get(pos.x, pos.y, pos.z)

    fun clear() {
        for (block in models) {
            block.solidFaces.clear()
            block.translucentFaces.clear()
        }
    }

    fun createVertexConsumer(pos: BlockPos, translucent: Boolean, atlas: NeoAtlas, offsetX: Float = 0f, offsetY: Float = 0f, offsetZ: Float = 0f): LightFace.Consumer {
        return LightFace.Consumer(
            (if (translucent) get(pos).translucentFaces else get(pos).solidFaces)::add,
            atlas,
            offsetX,
            offsetY,
            offsetZ
        )
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
        var `vibrancy$sectionMeshConsumer`: LightFace.Consumer?
    }
}