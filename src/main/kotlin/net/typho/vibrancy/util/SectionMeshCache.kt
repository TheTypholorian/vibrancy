package net.typho.vibrancy.util

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.shadows.LightFace

class SectionMeshCache(
    @JvmField
    val pos: SectionPos
) {
    @JvmField
    val models = Array(16) { Array(16) { Array(16) { Block(arrayListOf(), arrayListOf()) } } }

    fun get(x: Int, y: Int, z: Int) = models[x and 15][y and 15][z and 15]

    operator fun get(pos: IVec3<Int>) = get(pos.x, pos.y, pos.z)

    operator fun get(pos: BlockPos) = get(pos.x, pos.y, pos.z)

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
}