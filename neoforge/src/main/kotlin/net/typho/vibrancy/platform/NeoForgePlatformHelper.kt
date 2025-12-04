package net.typho.vibrancy.platform

import net.neoforged.fml.loading.FMLLoader
import net.typho.vibrancy.platform.services.PlatformHelper

class NeoForgePlatformHelper : PlatformHelper {
    override fun getPlatformName(): String {
        return "NeoForge"
    }

    override fun isDevelopmentEnvironment(): Boolean {
        return !FMLLoader.isProduction()
    }
}