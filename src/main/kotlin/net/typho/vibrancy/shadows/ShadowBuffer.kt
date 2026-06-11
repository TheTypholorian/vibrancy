package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.areaInclusive
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.sizeInclusive
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import org.lwjgl.system.MemoryUtil.memPutInt

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

    open class VoxelGrid(
        usage: GlBufferUsage
    ) : ShadowBuffer(usage) {
        @JvmField
        val gridBuffer = NeoGlBuffer()

        override fun free() {
            super.free()
            gridBuffer.free()
        }

        override fun lazyUpload(
            texWidth: Int,
            texHeight: Int,
            faces: List<PrimitiveQuad>
        ): Pair<AutoCloseable, () -> Unit> {
            throw UnsupportedOperationException("lazyUpload on ShadowBuffer.VoxelGrid")
        }

        override fun lazyUploadQuads(
            textures: List<GlTexture2D>,
            faces: List<Pair<PrimitiveQuad, Int>>
        ): () -> Unit {
            throw UnsupportedOperationException("lazyUploadQuads on ShadowBuffer.VoxelGrid")
        }

        open fun lazyUpload(texWidth: Int, texHeight: Int, numFaces: Int, bounds: AbstractRect3<Int>, faces: List<Pair<IVec3<Int>, List<PrimitiveQuad>>>): Pair<AutoCloseable, () -> Unit> {
            val quadBuffer = NeoBuffer.GCNative(numFaces * 4L * VERTEX_FORMAT.vertexSizeBytes)
            var numBlocks = 0

            quadBuffer.write().run {
                for ((pos, faces) in faces) {
                    if (faces.isNotEmpty()) {
                        numBlocks++
                    }

                    for (face in faces) {
                        face.apply { vertex ->
                            writeFloat(vertex.x)
                            writeFloat(vertex.y)
                            writeFloat(vertex.z)
                            writeInt(((vertex.u * texWidth).toInt() shl 16) or (vertex.v * texHeight).toInt())
                        }
                    }
                }
            }

            val gridBuffer = NeoBuffer.GCNative(32L + numBlocks * 16)

            gridBuffer.write().run {
                writeInt(bounds.min.x)
                writeInt(bounds.min.y)
                writeInt(bounds.min.z)
                writeInt(0)

                writeInt(bounds.sizeInclusive.x)
                writeInt(bounds.sizeInclusive.y)
                writeInt(bounds.sizeInclusive.z)
                writeInt(0)

                var quadIndex = 0

                for ((pos, faces) in faces) {
                    if (faces.isNotEmpty()) {
                        writeInt(pos.x)
                        writeInt(pos.y)
                        writeInt(pos.z)

                        writeShort(quadIndex)
                        quadIndex += faces.size
                        writeShort(quadIndex)
                    }
                }
            }

            return AutoCloseable {
                quadBuffer.free()
                gridBuffer.free()
            } to {
                size = numFaces
                bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(quadBuffer, usage) }
                this.gridBuffer.bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(gridBuffer, usage) }
            }
        }
    }
}