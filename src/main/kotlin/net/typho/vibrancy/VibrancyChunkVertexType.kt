package net.typho.vibrancy

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType

object VibrancyChunkVertexType : ChunkVertexType, ChunkVertexEncoder {
    // this format will only be used to get the stride, the only place that actually uses the format is ShaderChunkRenderer which we don't use
    @JvmField
    val format = GlVertexFormat(mapOf(), emptyArray(), 28)

    override fun write(
        ptr: Long,
        materialBits: Int,
        vertices: Array<ChunkVertexEncoder.Vertex>,
        section: Int
    ): Long {
        var ptr = ptr

        repeat(4) { i ->
            val vertex = vertices[i]
            MemoryIntrinsics.putFloat(ptr, vertex.x)
            MemoryIntrinsics.putFloat(ptr + 4, vertex.y)
            MemoryIntrinsics.putFloat(ptr + 8, vertex.z)
            MemoryIntrinsics.putFloat(ptr + 12, vertex.u)
            MemoryIntrinsics.putFloat(ptr + 16, vertex.v)
            MemoryIntrinsics.putInt(ptr + 20, vertex.color)
            MemoryIntrinsics.putInt(ptr + 24, 0x00FF0000) // TODO normals
            ptr += format.stride
        }

        return ptr
    }

    override fun getVertexFormat() = format

    override fun getEncoder() = this
}