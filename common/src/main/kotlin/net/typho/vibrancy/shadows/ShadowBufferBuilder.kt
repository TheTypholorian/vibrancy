package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import org.lwjgl.system.MemoryUtil.memPutFloat
import java.nio.ByteBuffer

open class ShadowBufferBuilder(val builder: ByteBufferBuilder) : VertexConsumer {
    companion object {
        const val VERTEX_SIZE = 8 * Float.SIZE_BYTES
        @JvmField
        val format: VertexFormat = DefaultVertexFormat.POSITION_TEX
        @JvmField
        val mode = VertexFormat.Mode.QUADS
    }

    protected var currentVertexPointer: Long? = null
    protected var numVertices = 0

    fun build(): ByteBuffer {
        val result = builder.build() ?: throw NullPointerException()

        return result.byteBuffer()
    }

    override fun addVertex(
        x: Float,
        y: Float,
        z: Float
    ): ShadowBufferBuilder {
        val ptr = builder.reserve(VERTEX_SIZE)
        currentVertexPointer = ptr
        memPutFloat(ptr, x)
        memPutFloat(ptr + 4, y)
        memPutFloat(ptr + 8, z)
        memPutFloat(ptr + 12, 0f)
        numVertices++
        return this
    }

    override fun setUv(u: Float, v: Float): ShadowBufferBuilder {
        val ptr = currentVertexPointer!!
        memPutFloat(ptr + 16, u)
        memPutFloat(ptr + 20, v)
        return this
    }

    override fun setColor(
        r: Int,
        g: Int,
        b: Int,
        a: Int
    ) = this

    override fun setUv1(u: Int, v: Int) = this

    override fun setUv2(u: Int, v: Int) = this

    override fun setNormal(
        x: Float,
        y: Float,
        z: Float
    ): ShadowBufferBuilder = this

    fun numQuads() = numVertices / 4
}