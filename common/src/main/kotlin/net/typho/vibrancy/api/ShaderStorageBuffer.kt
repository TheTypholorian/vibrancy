package net.typho.vibrancy.api

import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.system.NativeResource
import java.nio.ByteBuffer

data class ShaderStorageBuffer(val buffer: Int, val usage: Usage) : NativeResource {
    constructor(usage: Usage) : this(glGenBuffers(), usage)

    fun bind() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer)
    }

    fun bindBase(index: Int) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, buffer)
    }

    fun upload(buf: ByteBuffer) {
        glBufferData(GL_SHADER_STORAGE_BUFFER, buf.flip(), usage.id)
    }

    override fun free() {
        glDeleteBuffers(buffer)
    }

    companion object {
        fun unbind() {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
        }

        fun unbindBase(index: Int) {
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, 0)
        }
    }

    enum class Usage(val id: Int) {
        STATIC(GL_STATIC_DRAW),
        STREAM(GL_STREAM_DRAW)
    }
}