package net.typho.vibrancy

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.gui.IConfigScreenFactory

@Mod(value = Vibrancy.MOD_ID, dist = [Dist.CLIENT])
class VibrancyNeoForge(eventBus: IEventBus, modContainer: ModContainer) {
    init {
        modContainer.registerExtensionPoint(IConfigScreenFactory::class.java, IConfigScreenFactory { container, modListScreen -> VibrancyConfig.createScreen(modListScreen) })
    }
}