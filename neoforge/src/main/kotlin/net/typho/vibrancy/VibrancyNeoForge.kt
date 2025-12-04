package net.typho.vibrancy

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

@Mod(Constants.MOD_ID)
class VibrancyNeoForge(eventBus: IEventBus, mod: ModContainer) {
    init {
        Vibrancy.init()
    }
}