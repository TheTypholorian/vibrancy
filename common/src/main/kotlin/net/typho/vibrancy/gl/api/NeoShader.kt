package net.typho.vibrancy.gl.api

import com.mojang.blaze3d.shaders.AbstractUniform
import net.typho.vibrancy.gl.*
import net.typho.vibrancy.gl.error.ShaderCompileException
import net.typho.vibrancy.gl.error.ShaderLinkException
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import java.util.*

open class NeoShader(val id: Int) : Bindable<NeoShader.Bound> {
    private val uniforms = HashMap<String, Uniform?>()
    private val samplers = HashMap<String, Sampler>()
    private val bound = Bound()

    override fun bind(): Bound {
        type().bind(id)
        return bound
    }

    override fun bind(stack: GlResourceStack): Bound {
        stack.put(bound)
        return bind()
    }

    override fun type() = GlResourceType.PROGRAM

    override fun id(): Int = id

    inner class Bound : Unbindable, IShader {
        override fun getResource() = this@NeoShader

        override fun getUniform(name: String) = uniforms.computeIfAbsent(name) {
            val location = glGetUniformLocation(getResource().id(), name)

            if (location == -1) {
                return@computeIfAbsent null
            }

            return@computeIfAbsent Uniform(location)
        }

        override fun setSampler(name: String, id: Int) {
            samplers.computeIfAbsent(name) {
                Sampler(getUniform(name)!!, samplers.size)
            }.set(id)
        }
    }

    open class Builder() {
        val sources = LinkedList<Int>()

        fun attach(
            type: ShaderType,
            source: String
        ) {
            val id = glCreateShader(type.id)
            glShaderSource(id, source)
            glCompileShader(id)

            if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
                throw ShaderCompileException(glGetShaderInfoLog(id).trim())
            }

            sources.add(id)
        }

        fun build(): NeoShader {
            val id = glCreateProgram()

            for (source in sources) {
                glAttachShader(id, source)
            }

            glLinkProgram(id)

            if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
                throw ShaderLinkException(glGetProgramInfoLog(id).trim())
            }

            for (source in sources) {
                glDetachShader(id, source)
                glDeleteShader(source)
            }

            return NeoShader(id)
        }
    }

    open class Sampler(
        val uniform: Uniform,
        val unit: Int
    ) {
        fun set(id: Int) {
            GlResourceType.SAMPLERS[unit].bind(id)
            uniform.set(unit)
        }
    }

    open class Uniform(
        val location: Int
    ) : AbstractUniform() {
        override fun set(x: Float) {
            glUniform1f(location, x)
        }

        override fun set(x: Float, y: Float) {
            glUniform2f(location, x, y)
        }

        override fun set(x: Float, y: Float, z: Float) {
            glUniform3f(location, x, y, z)
        }

        override fun set(x: Float, y: Float, z: Float, w: Float) {
            glUniform4f(location, x, y, z, w)
        }

        override fun set(x: Int) {
            glUniform1i(location, x)
        }

        override fun set(x: Int, y: Int) {
            glUniform2i(location, x, y)
        }

        override fun set(x: Int, y: Int, z: Int) {
            glUniform3i(location, x, y, z)
        }

        override fun set(x: Int, y: Int, z: Int, w: Int) {
            glUniform4i(location, x, y, z, w)
        }

        override fun set(valueArray: FloatArray) {
            when (valueArray.size) {
                1 -> glUniform1fv(location, valueArray)
                2 -> glUniform2fv(location, valueArray)
                3 -> glUniform3fv(location, valueArray)
                4 -> glUniform4fv(location, valueArray)
                else -> glUniformMatrix4fv(location, false, valueArray)
            }
        }

        override fun set(vector: Vector3f) {
            glUniform3f(location, vector.x, vector.y, vector.z)
        }

        override fun set(vector: Vector4f) {
            glUniform4f(location, vector.x, vector.y, vector.z, vector.w)
        }

        override fun set(matrix: Matrix4f) {
            val buffer = FloatArray(16)
            matrix.get(buffer)
            glUniformMatrix4fv(location, false, buffer)
        }

        override fun set(matrix: Matrix3f) {
            val buffer = FloatArray(9)
            matrix.get(buffer)
            glUniformMatrix3fv(location, false, buffer)
        }

        override fun setSafe(x: Float, y: Float, z: Float, w: Float) {
            glUniform4f(location, x, y, z, w)
        }

        override fun setSafe(x: Int, y: Int, z: Int, w: Int) {
            glUniform4i(location, x, y, z, w)
        }

        override fun setMat2x2(m00: Float, m01: Float, m10: Float, m11: Float) {
            glUniformMatrix2fv(location, false, floatArrayOf(m00, m01, m10, m11))
        }

        override fun setMat2x3(m00: Float, m01: Float, m02: Float, m10: Float, m11: Float, m12: Float) {
            glUniformMatrix2x3fv(location, false, floatArrayOf(m00, m01, m02, m10, m11, m12))
        }

        override fun setMat2x4(m00: Float, m01: Float, m02: Float, m03: Float, m10: Float, m11: Float, m12: Float, m13: Float) {
            glUniformMatrix2x4fv(location, false, floatArrayOf(m00, m01, m02, m03, m10, m11, m12, m13))
        }

        override fun setMat3x2(m00: Float, m01: Float, m10: Float, m11: Float, m20: Float, m21: Float) {
            glUniformMatrix3x2fv(location, false, floatArrayOf(m00, m01, m10, m11, m20, m21))
        }

        override fun setMat3x3(m00: Float, m01: Float, m02: Float, m10: Float, m11: Float, m12: Float, m20: Float, m21: Float, m22: Float) {
            glUniformMatrix3fv(location, false, floatArrayOf(m00, m01, m02, m10, m11, m12, m20, m21, m22))
        }

        override fun setMat3x4(m00: Float, m01: Float, m02: Float, m03: Float, m10: Float, m11: Float, m12: Float, m13: Float, m20: Float, m21: Float, m22: Float, m23: Float) {
            glUniformMatrix3x4fv(location, false, floatArrayOf(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23))
        }

        override fun setMat4x2(m00: Float, m01: Float, m02: Float, m03: Float, m10: Float, m11: Float, m12: Float, m13: Float) {
            glUniformMatrix4x2fv(location, false, floatArrayOf(m00, m01, m02, m03, m10, m11, m12, m13))
        }

        override fun setMat4x3(m00: Float, m01: Float, m02: Float, m03: Float, m10: Float, m11: Float, m12: Float, m13: Float, m20: Float, m21: Float, m22: Float, m23: Float) {
            glUniformMatrix4x3fv(location, false, floatArrayOf(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23))
        }

        override fun setMat4x4(m00: Float, m01: Float, m02: Float, m03: Float, m10: Float, m11: Float, m12: Float, m13: Float, m20: Float, m21: Float, m22: Float, m23: Float, m30: Float, m31: Float, m32: Float, m33: Float) {
            glUniformMatrix4fv(location, false, floatArrayOf(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33))
        }
    }
}