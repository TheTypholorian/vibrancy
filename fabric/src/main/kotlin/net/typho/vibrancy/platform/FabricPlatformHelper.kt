package net.typho.vibrancy.platform

import net.fabricmc.loader.api.FabricLoader
import net.typho.vibrancy.platform.services.PlatformHelper
import java.nio.file.Path

class FabricPlatformHelper : PlatformHelper {
    override fun getPlatformName(): String {
        return "Fabric"
    }

    override fun isDevelopmentEnvironment(): Boolean {
        return FabricLoader.getInstance().isDevelopmentEnvironment
    }

    override fun getConfigDir(): Path = FabricLoader.getInstance().configDir
}