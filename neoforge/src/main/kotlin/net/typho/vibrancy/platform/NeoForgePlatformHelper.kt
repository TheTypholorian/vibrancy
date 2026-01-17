package net.typho.vibrancy.platform

import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.FMLPaths
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.platform.services.PlatformHelper
import java.nio.file.Path

@EventBusSubscriber(modid = Vibrancy.MOD_ID)
class NeoForgePlatformHelper : PlatformHelper {
    override fun getPlatformName(): String {
        return "NeoForge"
    }

    override fun isDevelopmentEnvironment(): Boolean {
        return !FMLLoader.isProduction()
    }

    override fun getConfigDir(): Path = FMLPaths.CONFIGDIR.get()
}