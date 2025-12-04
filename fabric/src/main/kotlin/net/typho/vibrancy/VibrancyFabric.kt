package net.typho.vibrancy

import net.fabricmc.api.ClientModInitializer

object VibrancyFabric : ClientModInitializer {
    override fun onInitializeClient() {
        Vibrancy.init()
    }
}