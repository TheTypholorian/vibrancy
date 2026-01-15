package net.typho.vibrancy.util

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component
import net.typho.vibrancy.Vibrancy
import java.util.function.Consumer

object VibrancyDebugKeys {
    const val KEY = InputConstants.KEY_F4

    @JvmStatic
    fun action(key: Int, feedback: Consumer<Component>) {
        when (key) {
            InputConstants.KEY_R -> {
                Vibrancy.LIGHT_MANAGER.rebuildAllShadows()
                feedback.accept(Component.translatable("debug.vibrancy.rebuild_all_shadows"))
            }
            InputConstants.KEY_E -> {
                Vibrancy.config.blockLights.raytraced.entityShadows = !Vibrancy.config.blockLights.raytraced.entityShadows
                Vibrancy.config.save()
                feedback.accept(Component.translatable("debug.vibrancy.toggle_entity_shadows"))
            }
        }
    }
}