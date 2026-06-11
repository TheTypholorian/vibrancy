package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.sizeInclusive
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer

open class VoxelGridBuffer(
    @JvmField
    val usage: GlBufferUsage
) : NeoGlBuffer() {
    open fun lazyUpload(texWidth: Int, texHeight: Int, numFaces: Int, bounds: AbstractRect3<Int>, faces: List<Pair<IVec3<Int>, List<PrimitiveQuad>>>): Pair<AutoCloseable, () -> Unit> {
        val gridBuffer = NeoBuffer.GCNative(32L + faces.size * 16)

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
            gridBuffer.free()
        } to {
            bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(gridBuffer, usage) }
        }
    }
}