package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer

open class ShadowBuffer(
    @JvmField
    val usage: GlBufferUsage
) : NeoGlBuffer() {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(4)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .padding(4)
            .build()
    }

    fun lazyUpload(faces: List<LightFace>): () -> Unit {
        if (faces.isEmpty()) {
            return {
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(0L, usage) }
            }
        } else {
            val buffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            buffer.write().run {
                faces.forEachIndexed { index, face ->
                    for (vertex in face.quad.vertices) {
                        writeFloat(vertex.pos.x)
                        writeFloat(vertex.pos.y)
                        writeFloat(vertex.pos.z)
                        writeInt(0)

                        writeFloat(vertex.textureUV!!.x)
                        writeFloat(vertex.textureUV!!.y)
                        writeInt(vertex.color!!.toRGBA())
                        writeInt(0)
                    }
                }
            }

            return {
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(buffer, usage) }
                buffer.free()
            }
        }
    }

    fun upload(faces: List<LightFace>) {
        lazyUpload(faces)()
    }
}