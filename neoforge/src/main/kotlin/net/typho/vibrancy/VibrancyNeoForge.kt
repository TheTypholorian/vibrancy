package net.typho.vibrancy

import me.shedaniel.autoconfig.AutoConfig
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingException
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.typho.big_shot_lib.api.client.rendering.opengl.GlQueue
import org.lwjgl.opengl.GL
import org.lwjgl.system.Platform

@Mod(value = Vibrancy.MOD_ID, dist = [Dist.CLIENT])
class VibrancyNeoForge(eventBus: IEventBus, modContainer: ModContainer) {
    init {
        modContainer.registerExtensionPoint(IConfigScreenFactory::class.java, IConfigScreenFactory { container, modListScreen -> AutoConfig.getConfigScreen(VibrancyConfig::class.java, modListScreen).get() })

        GlQueue.INSTANCE.queue {
            if (!GL.getCapabilities().GL_ARB_shader_storage_buffer_object) {
                throw ModLoadingException(
                    ModLoadingIssue.error(
                        if (Platform.get() == Platform.MACOSX)
                            "Vibrancy requires GL_ARB_shader_storage_buffer_object (OpenGL 4.3), which MacOS does not support, and there is no way to fix it."
                        else
                            "Vibrancy requires GL_ARB_shader_storage_buffer_object (OpenGL 4.3), which your system does not support. This can typically be solved by updating your GPU drivers."
                    )
                )
            }
        }
    }
}