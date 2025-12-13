package net.typho.vibrancy.gl

import org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL43.*

enum class ShaderType(
    val id: Int,
    val extension: String
) {
    VERTEX(GL_VERTEX_SHADER, "vsh"),
    GEOMETRY(GL_GEOMETRY_SHADER, "gsh"),
    FRAGMENT(GL_FRAGMENT_SHADER, "fsh"),
    COMPUTE(GL_COMPUTE_SHADER, "csh"),
    TESS_CONTROL(GL_TESS_CONTROL_SHADER, "tcsh"),
    TESS_EVALUATION(GL_TESS_EVALUATION_SHADER, "tesh"),
}