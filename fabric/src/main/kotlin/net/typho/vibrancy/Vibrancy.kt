package net.typho.vibrancy

import net.fabricmc.api.ModInitializer

object Vibrancy : ModInitializer {
    override fun onInitialize() {
        Constants.LOG.info("Hello Fabric world from Kotlin!")
        CommonObject.init()
    }
}