package net.typho.vibrancy.sodium;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.DefaultChunkMeshAttributes;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatAttribute;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;
import kotlin.math.floor

/**
 * @author Developers of Veil
 */
class VeilChunkVertex : ChunkVertexType {
    override fun getVertexFormat(): GlVertexFormat? {
        return VERTEX_FORMAT
    }

    override fun getEncoder(): ChunkVertexEncoder {
        return ChunkVertexEncoder { ptr: Long, materialBits: Int, vertices: Array<ChunkVertexEncoder.Vertex>?, section: Int ->
            var ptr = ptr
            var texCentroidU = 0.0f
            var texCentroidV = 0.0f

            for (vertex in vertices!!) {
                texCentroidU += vertex.u
                texCentroidV += vertex.v
            }

            texCentroidU *= 0.25f
            texCentroidV *= 0.25f

            for (i in 0..3) {
                val vertex = vertices[i]
                val x = quantizePosition(vertex.x)
                val y = quantizePosition(vertex.y)
                val z = quantizePosition(vertex.z)
                val u = encodeTexture(texCentroidU, vertex.u)
                val v = encodeTexture(texCentroidV, vertex.v)
                val light = encodeLight(vertex.light)
                MemoryUtil.memPutInt(ptr, packPositionHi(x, y, z))
                MemoryUtil.memPutInt(ptr + 4L, packPositionLo(x, y, z))
                MemoryUtil.memPutInt(ptr + 8L, ColorARGB.mulRGB(vertex.color, vertex.ao))
                MemoryUtil.memPutInt(ptr + 12L, packTexture(u, v))
                MemoryUtil.memPutInt(ptr + 16L, packLightAndData(light, materialBits, section))
                val ext = vertex as ChunkVertexEncoderVertexExtension
                MemoryUtil.memPutInt(ptr + 20L, ext.`vibrancy$getPackedNormal`())
                ptr += STRIDE.toLong()
            }
            ptr
        }
    }

    companion object {
        const val STRIDE: Int = 24
        val VERTEX_FORMAT: GlVertexFormat? = GlVertexFormat.builder(STRIDE)
            .addElement(DefaultChunkMeshAttributes.POSITION, 0, 0)
            .addElement(DefaultChunkMeshAttributes.COLOR, 1, 8)
            .addElement(DefaultChunkMeshAttributes.TEXTURE, 2, 12)
            .addElement(DefaultChunkMeshAttributes.LIGHT_MATERIAL_INDEX, 3, 16)
            .addElement(VertexFormatAttribute("NORMAL_INDEX", GlVertexAttributeFormat.BYTE, 4, true, false), 4, 20)
            .build()
        private const val POSITION_MAX_VALUE = 1048576
        private const val TEXTURE_MAX_VALUE = 32768
        private const val MODEL_ORIGIN = 8.0f
        private const val MODEL_RANGE = 32.0f

        private fun packPositionHi(x: Int, y: Int, z: Int): Int {
            return (x ushr 10 and 1023) or ((y ushr 10 and 1023) shl 10) or ((z ushr 10 and 1023) shl 20)
        }

        private fun packPositionLo(x: Int, y: Int, z: Int): Int {
            return (x and 1023) or ((y and 1023) shl 10) or ((z and 1023) shl 20)
        }

        private fun quantizePosition(position: Float): Int {
            return (normalizePosition(position) * POSITION_MAX_VALUE).toInt() and (POSITION_MAX_VALUE - 1)
        }

        private fun normalizePosition(v: Float): Float {
            return (MODEL_ORIGIN + v) / MODEL_RANGE
        }

        private fun packTexture(u: Int, v: Int): Int {
            return (u and 65535) or ((v and 65535) shl 16)
        }

        private fun encodeTexture(center: Float, x: Float): Int {
            val bias = if (x < center) 1 else -1
            val quantized = floorInt(x * TEXTURE_MAX_VALUE) + bias
            return quantized and (TEXTURE_MAX_VALUE - 1) or (sign(bias) shl 15)
        }

        private fun encodeLight(light: Int): Int {
            val sky = Mth.clamp(light ushr 16 and 0xFF, 8, 248)
            val block = Mth.clamp(light and 0xFF, 8, 248)
            return block or (sky shl 8)
        }

        private fun packLightAndData(light: Int, material: Int, section: Int): Int {
            return (light and 65535) or ((material and 0xFF) shl 16) or ((section and 0xFF) shl 24)
        }

        private fun sign(x: Int): Int {
            return x ushr 31
        }

        private fun floorInt(x: Float): Int {
            return floor(x.toDouble()).toInt()
        }
    }
}
