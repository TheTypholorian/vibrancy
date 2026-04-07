package net.typho.vibrancy.util

import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil.NULL
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object GlThreadPool : ThreadPoolExecutor(4, 6, 1L, TimeUnit.HOURS, SynchronousQueue(), ThreadFactory { task ->
    Thread {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        val handle = glfwCreateWindow(1, 1, "Vibrancy GlThreadPool context", NULL, Minecraft.getInstance().window.window)

        if (handle == NULL) {
            throw NullPointerException("Couldn't create GlThreadPool window")
        }

        glfwMakeContextCurrent(handle)
        GL.createCapabilities()

        task.run()

        glfwDestroyWindow(handle)
    }
})