package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
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
import org.lwjgl.system.MemoryUtil.memSet

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

            quadBuffer.write().run {
                for ((pos, faces) in faces) {
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

            val gridBufferSize = 28L + bounds.areaInclusive * 2 * Int.SIZE_BYTES
            val gridBuffer = NeoBuffer.GCNative(gridBufferSize)

            memSet(gridBuffer.address, 0, gridBufferSize)

            memPutInt(gridBuffer.address, bounds.min.x)
            memPutInt(gridBuffer.address + 4L, bounds.min.y)
            memPutInt(gridBuffer.address + 8L, bounds.min.z)

            memPutInt(gridBuffer.address + 16L, bounds.sizeInclusive.x)
            memPutInt(gridBuffer.address + 20L, bounds.sizeInclusive.y)
            memPutInt(gridBuffer.address + 24L, bounds.sizeInclusive.z)

            var quadIndex = 0

            for ((pos, faces) in faces) {
                if (faces.isNotEmpty()) {
                    val index = 28L + (((pos.x - bounds.min.x) * bounds.sizeInclusive.y + (pos.y - bounds.min.y)) * bounds.sizeInclusive.z + (pos.z - bounds.min.z)) * 2 * Int.SIZE_BYTES

                    if (index < 0 || index > gridBufferSize) {
                        throw IndexOutOfBoundsException("$index $gridBufferSize $pos ${bounds.min} ${bounds.sizeInclusive}")
                    }

                    val bytePointer = gridBuffer.address + index

                    memPutInt(bytePointer, quadIndex)
                    quadIndex += faces.size
                    memPutInt(bytePointer + 4L, quadIndex)
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