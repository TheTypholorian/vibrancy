package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
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

    fun lazyUpload(texWidth: Int, texHeight: Int, faces: List<LightFace>): Pair<AutoCloseable, () -> Unit> {
        if (faces.isEmpty()) {
            return AutoCloseable { } to {
                size = 0
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(0L, usage) }
            }
        } else {
            val buffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            buffer.write().run {
                faces.forEachIndexed { index, face ->
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

    fun lazyUploadQuads(texWidth: Int, texHeight: Int, faces: List<NeoBakedQuad>): Pair<AutoCloseable, () -> Unit> {
        if (faces.isEmpty()) {
            return AutoCloseable { } to {
                size = 0
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(0L, usage) }
            }
        } else {
            val buffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            buffer.write().run {
                faces.forEachIndexed { index, quad ->
                    for (vertex in quad.vertices) {
                        writeFloat(vertex.pos.x)
                        writeFloat(vertex.pos.y)
                        writeFloat(vertex.pos.z)
                        writeInt(((vertex.textureUV!!.x * texWidth).toInt() shl 16) or (vertex.textureUV!!.y * texHeight).toInt())
                    }
                }
            }

            return buffer to {
                size = faces.size
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(buffer, usage) }
            }
        }
    }

    fun lazyUploadQuads(textures: List<GlTexture2D>, faces: List<Pair<NeoBakedQuad, Int>>): () -> Unit {
        if (faces.isEmpty()) {
            return {
                size = 0
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(0L, usage) }
            }
        } else {
            val buffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            buffer.write().run {
                faces.forEachIndexed { index, quad ->
                    val texture = textures[quad.second]

                    for (vertex in quad.first.vertices) {
                        writeFloat(vertex.pos.x)
                        writeFloat(vertex.pos.y)
                        writeFloat(vertex.pos.z)
                        writeInt(((vertex.textureUV!!.x * texture.width!!).toInt() shl 16) or (vertex.textureUV!!.y * texture.height!!).toInt())
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