package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT
import org.lwjgl.opengl.GL30.nglMapBufferRange
import org.lwjgl.opengl.GL44.GL_MAP_COHERENT_BIT
import org.lwjgl.opengl.GL44.GL_MAP_PERSISTENT_BIT
import org.lwjgl.opengl.GL44.glBufferStorage
import org.lwjgl.system.MemoryUtil

open class ShadowBuffer(
    @JvmField
    val usage: GlBufferUsage
) : NeoGlBuffer() {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.OVERLAY_UV)
            .build()
    }

    var sizeBytes: Long = 0
        protected set

    open fun lazyUpload(texWidth: Int, texHeight: Int, faces: List<LightFace>): Pair<AutoCloseable, () -> Unit> {
        if (faces.isEmpty()) {
            return AutoCloseable { } to {
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
                        writeInt(((vertex.textureUV!!.x * texWidth).toInt() shl 16) or (vertex.textureUV!!.y * texHeight).toInt())
                    }
                }
            }

            return buffer to {
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(buffer, usage) }
                sizeBytes = buffer.size
            }
        }
    }

    open fun lazyUploadQuads(texWidth: Int, texHeight: Int, faces: List<NeoBakedQuad>): Pair<AutoCloseable, () -> Unit> {
        if (faces.isEmpty()) {
            return AutoCloseable { } to {
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
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(buffer, usage) }
                sizeBytes = buffer.size
            }
        }
    }

    open fun lazyUploadQuads(textures: List<GlTexture2D>, faces: List<Pair<NeoBakedQuad, Int>>): () -> Unit {
        if (faces.isEmpty()) {
            return {
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
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(buffer, usage) }
                sizeBytes = buffer.size
                buffer.free()
            }
        }
    }

    open class PersistentMapped(
        allocate: Long
    ) : ShadowBuffer(GlBufferUsage.STREAM_DRAW) {
        @JvmField
        val mapped = bind(GlBufferTarget.ARRAY_BUFFER).use {
            // TODO
            val access = GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT
            glBufferStorage(GL_ARRAY_BUFFER, allocate, access)
            val pointer = nglMapBufferRange(GL_ARRAY_BUFFER, 0, allocate, access)

            if (pointer == MemoryUtil.NULL) {
                throw NullPointerException("Failed to map buffer GL_ARRAY_BUFFER")
            }

            return@use NeoBuffer.Native(pointer, allocate)
        }

        override fun free() {
            bind(GlBufferTarget.ARRAY_BUFFER).use { it.unmapBuffer() }
            super.free()
        }

        override fun lazyUpload(texWidth: Int, texHeight: Int, faces: List<LightFace>): Pair<AutoCloseable, () -> Unit> {
            if (faces.isEmpty()) {
                return AutoCloseable { } to {
                    sizeBytes = 0
                }
            } else {
                return AutoCloseable { } to {
                    mapped.write().run {
                        faces.forEachIndexed { index, face ->
                            for (vertex in face.quad.vertices) {
                                writeFloat(vertex.pos.x)
                                writeFloat(vertex.pos.y)
                                writeFloat(vertex.pos.z)
                                writeInt(((vertex.textureUV!!.x * texWidth).toInt() shl 16) or (vertex.textureUV!!.y * texHeight).toInt())
                            }
                        }
                    }
                    sizeBytes = faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes
                }
            }
        }

        override fun lazyUploadQuads(texWidth: Int, texHeight: Int, faces: List<NeoBakedQuad>): Pair<AutoCloseable, () -> Unit> {
            if (faces.isEmpty()) {
                return AutoCloseable { } to {
                    sizeBytes = 0
                }
            } else {
                return AutoCloseable { } to {
                    mapped.write().run {
                        faces.forEachIndexed { index, face ->
                            for (vertex in face.vertices) {
                                writeFloat(vertex.pos.x)
                                writeFloat(vertex.pos.y)
                                writeFloat(vertex.pos.z)
                                writeInt(((vertex.textureUV!!.x * texWidth).toInt() shl 16) or (vertex.textureUV!!.y * texHeight).toInt())
                            }
                        }
                    }
                    sizeBytes = faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes
                }
            }
        }

        override fun lazyUploadQuads(textures: List<GlTexture2D>, faces: List<Pair<NeoBakedQuad, Int>>): () -> Unit {
            if (faces.isEmpty()) {
                return {
                    sizeBytes = 0
                }
            } else {
                return {
                    mapped.write().run {
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
                    sizeBytes = faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes
                }
            }
        }
    }
}