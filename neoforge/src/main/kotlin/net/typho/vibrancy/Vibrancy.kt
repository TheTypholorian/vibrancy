package net.typho.vibrancy

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

@Mod(Constants.MOD_ID)
class Vibrancy(eventBus: IEventBus, modContainer: ModContainer) {
    init {
        Constants.LOG.info("Hello NeoForge world from Kotlin!")
        CommonObject.init()
    }
}