package net.typho.vibrancy.util

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
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

    fun createVertexConsumer(pos: BlockPos, translucent: Boolean, atlas: NeoAtlas, offsetX: Float = 0f, offsetY: Float = 0f, offsetZ: Float = 0f): NeoBakedQuad.Consumer {
        val faces = if (translucent) get(pos).translucentFaces else get(pos).solidFaces
        return object : NeoBakedQuad.Consumer() {
            override fun take(quad: NeoBakedQuad) {
                faces.add(LightFace(
                    pos,
                    quad,
                    atlas
                ))
            }

            override fun vertex(x: Float, y: Float, z: Float): NeoVertexConsumer {
                return super.vertex(x + offsetX, y + offsetY, z + offsetZ)
            }
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
        var `vibrancy$sectionMeshConsumer`: NeoBakedQuad.Consumer?
    }
}