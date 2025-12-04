package net.typho.vibrancy.platform

import net.typho.vibrancy.platform.services.PlatformHelper
import net.fabricmc.loader.api.FabricLoader

class FabricPlatformHelper : PlatformHelper {
    override fun getPlatformName(): String {
        return "Fabric"
    }

    override fun isModLoaded(modId: String?): Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }

    override fun isDevelopmentEnvironment(): Boolean {
        return FabricLoader.getInstance().isDevelopmentEnvironment
    }
}