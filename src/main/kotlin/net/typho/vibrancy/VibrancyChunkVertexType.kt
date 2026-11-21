package net.typho.vibrancy

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics
import net.caffeinemc.mods.sodium.api.util.ColorARGB
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeFormat
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.DefaultChunkMeshAttributes
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatAttribute
import net.minecraft.util.Mth
import kotlin.math.roundToInt

object VibrancyChunkVertexType : ChunkVertexType, ChunkVertexEncoder {
    @JvmField
    val format = GlVertexFormat.builder(23)
        .addElement(DefaultChunkMeshAttributes.POSITION, 0, 0)
        .addElement(DefaultChunkMeshAttributes.COLOR, 1, 8)
        .addElement(DefaultChunkMeshAttributes.TEXTURE, 2, 12)
        .addElement(DefaultChunkMeshAttributes.LIGHT_MATERIAL_INDEX, 3, 16)
        .addElement(VertexFormatAttribute("NORMAL", GlVertexAttributeFormat.UNSIGNED_BYTE, 3, true, true), 4, 20)
        .build()

    private fun packPositionHi(x: Int, y: Int, z: Int): Int {
        return (x ushr 10 and 1023) shl 0 or ((y ushr 10 and 1023) shl 10) or ((z ushr 10 and 1023) shl 20)
    }

    private fun packPositionLo(x: Int, y: Int, z: Int): Int {
        return (x and 1023) shl 0 or ((y and 1023) shl 10) or ((z and 1023) shl 20)
    }

    private fun quantizePosition(position: Float): Int {
        return (normalizePosition(position) * 1048576.0f).toInt() and 1048575
    }

    private fun normalizePosition(v: Float): Float {
        return (8.0f + v) / 32.0f
    }

    private fun packTexture(u: Int, v: Int): Int {
        return (u and '\uffff'.code) shl 0 or ((v and '\uffff'.code) shl 16)
    }

    private fun encodeTexture(center: Float, x: Float): Int {
        val bias = if (x < center) 1 else -1
        val quantized = (x * 32768.0f).roundToInt() + bias
        return quantized and 32767 or (sign(bias) shl 15)
    }

    private fun encodeLight(light: Int): Int {
        val sky = Mth.clamp((light ushr 16 and 255) + 8, 8, 248)
        val block = Mth.clamp((light ushr 0 and 255) + 8, 8, 248)
        return block shl 0 or (sky shl 8)
    }

    private fun packLightAndData(light: Int, material: Int, section: Int): Int {
        return (light and 0xFFFF) shl 0 or ((material and 255) shl 16) or ((section and 255) shl 24)
    }

    private fun sign(x: Int): Int {
        return x ushr 31
    }

    override fun write(
        ptr: Long,
        materialBits: Int,
        vertices: Array<ChunkVertexEncoder.Vertex>,
        section: Int
    ): Long {
        var ptr = ptr
        var texCentroidU = 0f
        var texCentroidV = 0f

        for (vertex in vertices) {
            texCentroidU += vertex.u
            texCentroidV += vertex.v
        }

        texCentroidU *= 0.25f
        texCentroidV *= 0.25f

        repeat(4) { i ->
            val vertex = vertices[i]
            val x = quantizePosition(vertex.x)
            val y = quantizePosition(vertex.y)
            val z = quantizePosition(vertex.z)
            val u = encodeTexture(texCentroidU, vertex.u)
            val v = encodeTexture(texCentroidV, vertex.v)
            val light = encodeLight(vertex.light)
            MemoryIntrinsics.putInt(ptr, packPositionHi(x, y, z))
            MemoryIntrinsics.putInt(ptr + 4, packPositionLo(x, y, z))
            MemoryIntrinsics.putInt(ptr + 8, ColorARGB.mulRGB(vertex.color, vertex.ao))
            MemoryIntrinsics.putInt(ptr + 12, packTexture(u, v))
            MemoryIntrinsics.putInt(ptr + 16, packLightAndData(light, materialBits, section))
            MemoryIntrinsics.putInt(ptr + 20, 0x00FF00) // TODO normals
            ptr += 24
        }

        return ptr
    }

    override fun getVertexFormat() = format

    override fun getEncoder() = this
}