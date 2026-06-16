package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer

open class ShadowBuffer(
    @JvmField
    val usage: GlBufferUsage
) : NeoGlBuffer() {
    var size: Int = 0
        protected set

    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.OVERLAY_UV)
            .build()
    }

    open fun lazyUpload(texWidth: Int, texHeight: Int, faces: List<PrimitiveQuad>): Pair<AutoCloseable, () -> Unit> {
        if (faces.isEmpty()) {
            return AutoCloseable { } to {
                size = 0
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(0L, usage) }
            }
        } else {
            val buffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            buffer.write().run {
                for (face in faces) {
                    face.apply { vertex ->
                        writeFloat(vertex.x)
                        writeFloat(vertex.y)
                        writeFloat(vertex.z)
                        writeInt(((vertex.u * texWidth).toInt() shl 16) or (vertex.v * texHeight).toInt())
                    }
                }
            }

            return buffer to {
                size = faces.size
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(buffer, usage) }
            }
        }
    }

    open fun lazyUploadQuads(textures: List<GlTexture2D>, faces: List<Pair<PrimitiveQuad, Int>>): () -> Unit {
        if (faces.isEmpty()) {
            return {
                size = 0
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(0L, usage) }
            }
        } else {
            val buffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            buffer.write().run {
                for (face in faces) {
                    val texture = textures[face.second]

                    face.first.apply { vertex ->
                        writeFloat(vertex.x)
                        writeFloat(vertex.y)
                        writeFloat(vertex.z)
                        writeInt(((vertex.u * texture.width!!).toInt() shl 16) or (vertex.v * texture.height!!).toInt())
                    }
                }
            }

            return {
                size = faces.size
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(buffer, usage) }
                buffer.free()
            }
        }
    }
}