package net.typho.vibrancy.util

import net.typho.big_shot_lib.api.client.rendering.common.GpuTexture
import net.typho.big_shot_lib.api.client.rendering.util.PackedNormal
import net.typho.big_shot_lib.api.client.rendering.util.mesh.PrimitiveQuad
import net.typho.big_shot_lib.api.client.rendering.util.mesh.PrimitiveVertex
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.ceil

open class BlockFace(
    v0: PrimitiveVertex,
    v1: PrimitiveVertex,
    v2: PrimitiveVertex,
    v3: PrimitiveVertex,
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) : PrimitiveQuad(v0, v1, v2, v3) {
    constructor(
        v0: PrimitiveVertex,
        v1: PrimitiveVertex,
        v2: PrimitiveVertex,
        v3: PrimitiveVertex,
        atlas: GpuTexture
    ) : this(
        v0,
        v1,
        v2,
        v3,
        ceil(abs(v0.u - v2.u) * atlas.width).toInt(),
        ceil(abs(v0.v - v2.v) * atlas.height).toInt()
    )

    override fun copyWithOffset(x: Float, y: Float, z: Float): BlockFace {
        return BlockFace(
            PrimitiveVertex(v0, x, y, z),
            PrimitiveVertex(v1, x, y, z),
            PrimitiveVertex(v2, x, y, z),
            PrimitiveVertex(v3, x, y, z),
            width,
            height
        )
    }

    override fun copyWithOffset(x: Int, y: Int, z: Int): BlockFace {
        return copyWithOffset(x.toFloat(), y.toFloat(), z.toFloat())
    }

    fun pointsToward(x: Float, y: Float, z: Float): Boolean {
        val delta = Vector3f(x - v0.x, y - v0.y, z - v0.z).normalize()
        return Vector3f(PackedNormal.unpackX(v0.normal), PackedNormal.unpackY(v0.normal), PackedNormal.unpackZ(v0.normal)).dot(delta) > 0
    }

    open class Consumer(
        out: (face: BlockFace) -> Unit,
        @JvmField
        val atlas: GpuTexture,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        offsetZ: Float = 0f,
    ) : PrimitiveQuad.Consumer({ out(it as BlockFace) }, offsetX, offsetY, offsetZ) {
        override fun create(): PrimitiveQuad {
            return BlockFace(v0, v1, v2, v3, atlas)
        }
    }
}