package net.typho.vibrancy.util

import com.mojang.blaze3d.platform.GlDebug
import net.minecraft.client.Minecraft
import net.typho.big_shot_lib.api.client.rendering.opengl.state.NeoGlStateManager
import net.typho.big_shot_lib.api.client.rendering.util.ContextLocal
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil.NULL
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private val ctxLock = Any()

object GlThreadPool : ThreadPoolExecutor(4, 4, 10L, TimeUnit.MINUTES, LinkedBlockingQueue(), ThreadFactory { task ->
    val main = Minecraft.getInstance().window.window
    glfwMakeContextCurrent(main)

    val handle = synchronized(ctxLock) {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        glfwCreateWindow(1, 1, "Vibrancy GlThreadPool context", NULL, main)
    }

    if (handle == NULL) {
        throw NullPointerException("Couldn't create GlThreadPool window")
    }

    glfwMakeContextCurrent(handle)
    val cap = GL.createCapabilities()
    GlDebug.enableDebugCallback(1, true)

    val thread = Thread {
        glfwMakeContextCurrent(handle)
        GL.setCapabilities(cap)

        NeoGlStateManager.THREAD_LOCAL.set(NeoGlStateManager.Standalone())
        ContextLocal.CLEANERS[handle] = { it.free() }

        try {
            task.run()
        } finally {
            glfwMakeContextCurrent(NULL)
            GL.setCapabilities(null)
            glfwDestroyWindow(handle)
            NeoGlStateManager.THREAD_LOCAL.remove()
            ContextLocal.CLEANERS.remove(handle)
        }
    }

    glfwMakeContextCurrent(main)

    return@ThreadFactory thread
})