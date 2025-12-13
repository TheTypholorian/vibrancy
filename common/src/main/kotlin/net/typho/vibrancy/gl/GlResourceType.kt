package net.typho.vibrancy.gl

import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL31.*
import org.lwjgl.opengl.GL40.*
import org.lwjgl.opengl.GL43.*
import org.lwjgl.opengl.GL44.GL_QUERY_BUFFER
import org.lwjgl.opengl.GL45.glBindTextureUnit
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.IntStream

data class GlResourceType(
    val glName: Int,
    private val bind: Consumer<Int>,
    private val unbind: Runnable
) {
    constructor(
        name: Int,
        bind: BiConsumer<Int, Int>
    ) : this(
        name,
        { bind.accept(name, it) },
        { bind.accept(name, 0) }
    )

    constructor(
        name: Int,
        bind: Consumer<Int>
    ) : this(
        name,
        { bind.accept(it) },
        { bind.accept(0) }
    )

    fun bind(id: Int) = bind.accept(id)

    fun unbind() = unbind.run()

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "GlResourceType[${glName.toHexString()}]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GlResourceType

        return glName == other.glName
    }

    override fun hashCode(): Int {
        return glName
    }

    companion object {
        val ARRAY_BUFFER = GlResourceType(GL_ARRAY_BUFFER, ::glBindBuffer)
        val ELEMENT_ARRAY_BUFFER = GlResourceType(GL_ELEMENT_ARRAY_BUFFER, ::glBindBuffer)
        val PIXEL_PACK_BUFFER = GlResourceType(GL_PIXEL_PACK_BUFFER, ::glBindBuffer)
        val PIXEL_UNPACK_BUFFER = GlResourceType(GL_PIXEL_UNPACK_BUFFER, ::glBindBuffer)
        val UNIFORM_BUFFER = GlResourceType(GL_UNIFORM_BUFFER, ::glBindBuffer)
        val TEXTURE_BUFFER_STORAGE = GlResourceType(GL_TEXTURE_BUFFER, ::glBindBuffer)
        val TRANSFORM_FEEDBACK_BUFFER = GlResourceType(GL_TRANSFORM_FEEDBACK_BUFFER, ::glBindBuffer)
        val COPY_READ_BUFFER = GlResourceType(GL_COPY_READ_BUFFER, ::glBindBuffer)
        val COPY_WRITE_BUFFER = GlResourceType(GL_COPY_WRITE_BUFFER, ::glBindBuffer)
        val DRAW_INDIRECT_BUFFER = GlResourceType(GL_DRAW_INDIRECT_BUFFER, ::glBindBuffer)
        val ATOMIC_COUNTER_BUFFER = GlResourceType(GL_ATOMIC_COUNTER_BUFFER, ::glBindBuffer)
        val DISPATCH_INDIRECT_BUFFER = GlResourceType(GL_DISPATCH_INDIRECT_BUFFER, ::glBindBuffer)
        val SHADER_STORAGE_BUFFER = GlResourceType(GL_SHADER_STORAGE_BUFFER, ::glBindBuffer)
        val QUERY_BUFFER = GlResourceType(GL_QUERY_BUFFER, ::glBindBuffer)

        val VERTEX_ARRAY = GlResourceType(GL_VERTEX_ARRAY, ::glBindVertexArray)

        val TEXTURE_1D = GlResourceType(GL_TEXTURE_1D, ::glBindTexture)
        val TEXTURE_2D = GlResourceType(GL_TEXTURE_2D, ::glBindTexture)
        val TEXTURE_2D_MULTISAMPLE = GlResourceType(GL_TEXTURE_2D_MULTISAMPLE, ::glBindTexture)
        val TEXTURE_3D = GlResourceType(GL_TEXTURE_3D, ::glBindTexture)
        val TEXTURE_CUBE_MAP = GlResourceType(GL_TEXTURE_CUBE_MAP, ::glBindTexture)
        val TEXTURE_RECTANGLE = GlResourceType(GL_TEXTURE_RECTANGLE, ::glBindTexture)
        val TEXTURE_BUFFER_VIEW = GlResourceType(GL_TEXTURE_BUFFER, ::glBindTexture)
        val TEXTURE_1D_ARRAY = GlResourceType(GL_TEXTURE_1D_ARRAY, ::glBindTexture)
        val TEXTURE_2D_ARRAY = GlResourceType(GL_TEXTURE_2D_ARRAY, ::glBindTexture)
        val TEXTURE_2D_MULTISAMPLE_ARRAY = GlResourceType(GL_TEXTURE_2D_MULTISAMPLE_ARRAY, ::glBindTexture)
        val TEXTURE_CUBE_MAP_ARRAY = GlResourceType(GL_TEXTURE_CUBE_MAP_ARRAY, ::glBindTexture)

        val TEXTURE_UNITS = array(
            GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS,
            { it },
            ::glBindTextureUnit
        )
        val SAMPLERS = array(
            GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS,
            { GL_TEXTURE0 + it },
            ::glBindSampler
        )

        val FRAMEBUFFER = GlResourceType(GL_FRAMEBUFFER, ::glBindFramebuffer)
        val READ_FRAMEBUFFER = GlResourceType(GL_READ_FRAMEBUFFER, ::glBindFramebuffer)
        val DRAW_FRAMEBUFFER = GlResourceType(GL_DRAW_FRAMEBUFFER, ::glBindFramebuffer)

        val RENDERBUFFER = GlResourceType(GL_RENDERBUFFER, ::glBindRenderbuffer)

        val TRANSFORM_FEEDBACK = GlResourceType(GL_TRANSFORM_FEEDBACK, ::glBindTransformFeedback)

        val PROGRAM = GlResourceType(GL_PROGRAM, ::glUseProgram)
        val PROGRAM_PIPELINE = GlResourceType(GL_PROGRAM_PIPELINE, ::glBindProgramPipeline)

        fun array(
            number: Int,
            glName: Function<Int, Int>,
            bind: BiConsumer<Int, Int>
        ): Array<GlResourceType> {
            return IntStream.range(0, number - 1)
                .mapToObj { GlResourceType(glName.apply(it), bind) }
                .toArray { arrayOf() }
        }
    }
}