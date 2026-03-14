package net.typho.vibrancy

import net.fabricmc.api.ClientModInitializer
import net.typho.big_shot_lib.api.client.opengl.util.OpenGL
import org.lwjgl.opengl.GL
import org.lwjgl.system.Platform

object VibrancyOpenGLChecker : ClientModInitializer {
    override fun onInitializeClient() {
        OpenGL.INSTANCE.recordRenderCall {
            if (!GL.getCapabilities().GL_ARB_shader_storage_buffer_object) {
                val text = if (Platform.get() == Platform.MACOSX)
                    "Vibrancy requires GL_ARB_shader_storage_buffer_object (OpenGL 4.3), which MacOS does not support, and there is no way to fix it."
                else
                    "Vibrancy requires GL_ARB_shader_storage_buffer_object (OpenGL 4.3), which your system does not support. This can typically be solved by updating your GPU drivers."
                throw UnsupportedOperationException(text)
            }
        }
    }
}