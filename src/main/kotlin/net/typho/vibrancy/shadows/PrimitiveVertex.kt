package net.typho.vibrancy.shadows

import net.minecraft.client.renderer.LightTexture

data class PrimitiveVertex(
    @JvmField
    var x: Float,
    @JvmField
    var y: Float,
    @JvmField
    var z: Float,
    @JvmField
    var color: Int,
    @JvmField
    var u: Float,
    @JvmField
    var v: Float,
    @JvmField
    var light: Int,
    @JvmField
    var normal: Int
) {
    constructor() : this(0f, 0f, 0f, 0, 0f, 0f, 0, 0)

    constructor(other: PrimitiveVertex, offsetX: Float, offsetY: Float, offsetZ: Float) : this(
        other.x + offsetX,
        other.y + offsetY,
        other.z + offsetZ,
        other.color,
        other.u,
        other.v,
        other.light,
        other.normal
    )

    override fun toString(): String {
        return "Vertex(pos=($x, $y, $z), color=0x${color.toHexString()}, uv=($u, $v), light=(block=${LightTexture.block(light)}, sky=${LightTexture.sky(light)}), normal=$normal)"
    }
}