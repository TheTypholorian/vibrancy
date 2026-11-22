package net.typho.vibrancy.collection

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType
import net.minecraft.core.BlockPos
import net.typho.vibrancy.ext.ChunkVertexExt

object VibrancyChunkVertexType : ChunkVertexType, ChunkVertexEncoder {
    // this object will only be used to get the stride, the only place that actually uses the format is ShaderChunkRenderer which we don't use
    @JvmField
    val format = GlVertexFormat(mapOf(), emptyArray(), 28)
    @JvmField
    var lightPos: BlockPos? = null // TODO
    @JvmField
    var blockPos: BlockPos? = null

    override fun write(
        ptr: Long,
        materialBits: Int,
        vertices: Array<ChunkVertexEncoder.Vertex>,
        section: Int
    ): Long {
        var ptr = ptr

        repeat(4) { i ->
            val vertex = vertices[i]
            var x = vertex.x
            var y = vertex.y
            var z = vertex.z

            blockPos?.let { blockPos ->
                lightPos?.let { lightPos ->
                    x += (-(blockPos.x and 15) + blockPos.x - lightPos.x).toFloat()
                    y += (-(blockPos.y and 15) + blockPos.y - lightPos.y).toFloat()
                    z += (-(blockPos.z and 15) + blockPos.z - lightPos.z).toFloat()
                }
            }

            MemoryIntrinsics.putFloat(ptr, x)
            MemoryIntrinsics.putFloat(ptr + 4, y)
            MemoryIntrinsics.putFloat(ptr + 8, z)
            MemoryIntrinsics.putFloat(ptr + 12, vertex.u)
            MemoryIntrinsics.putFloat(ptr + 16, vertex.v)
            MemoryIntrinsics.putInt(ptr + 20, vertex.color)
            MemoryIntrinsics.putInt(ptr + 24, (vertex as ChunkVertexExt).`vibrancy$getNormal`())
            ptr += format.stride
        }

        return ptr
    }

    override fun getVertexFormat() = format

    override fun getEncoder() = this
}