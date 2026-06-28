package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.Recyclable
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.big_shot_lib.api.client.rendering.util.mesh.PrimitiveQuad
import net.typho.big_shot_lib.api.math.IRect3
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.big_shot_lib.api.util.buffer.MemoryPointer

open class VoxelGridBuffer(
    val usage: GpuBufferUsage
) : Recyclable {
    var buffer: GpuBuffer? = null
        protected set

    override fun recycle() {
        buffer?.recycle()
        buffer = null
    }

    open fun lazyUpload(texWidth: Int, texHeight: Int, numFaces: Int, bounds: IRect3<Int>, faces: List<Pair<IVec3<Int>, List<PrimitiveQuad>>>): Pair<AutoCloseable, () -> Unit> {
        val gridBuffer = MemoryPointer.alloc(32L + faces.size * 16) { "Voxel Grid Buffer" }

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
            val buffer = GpuObjects.buffer(null, gridBuffer.size, usage)
            buffer.upload(gridBuffer)
            this.buffer = buffer
        }
    }
}