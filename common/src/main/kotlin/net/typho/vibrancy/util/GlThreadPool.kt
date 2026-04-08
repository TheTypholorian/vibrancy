package net.typho.vibrancy.util

import com.mojang.blaze3d.platform.GlDebug
import net.minecraft.client.Minecraft
import net.typho.big_shot_lib.api.client.rendering.opengl.state.NeoGlStateManager
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil.NULL
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object GlThreadPool : ThreadPoolExecutor(4, 4, 1L, TimeUnit.HOURS, LinkedBlockingQueue(), ThreadFactory { task ->
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
    val handle = glfwCreateWindow(1, 1, "Vibrancy GlThreadPool context", NULL, Minecraft.getInstance().window.window)

    if (handle == NULL) {
        throw NullPointerException("Couldn't create GlThreadPool window")
    }

    Thread {
        glfwMakeContextCurrent(handle)
        GL.createCapabilities()
        GlDebug.enableDebugCallback(1, true)

        NeoGlStateManager.THREAD_LOCAL.set(NeoGlStateManager.Standalone())

        try {
            task.run()
        } finally {
            glfwDestroyWindow(handle)
            NeoGlStateManager.THREAD_LOCAL.remove()
        }
    }
})