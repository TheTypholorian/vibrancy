package com.example.examplemod

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

@Mod(Constants.MOD_ID)
class ExampleMod(eventBus: IEventBus, modContainer: ModContainer) {
    init {
        Constants.LOG.info("Hello NeoForge world from Kotlin!")
        CommonObject.init()
    }
}