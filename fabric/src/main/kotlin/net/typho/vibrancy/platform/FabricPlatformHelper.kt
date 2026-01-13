package net.typho.vibrancy.platform

import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.typho.vibrancy.Vibrancy
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

    override fun registerResourcePack(name: String) {
        ResourceManagerHelper.registerBuiltinResourcePack(
            Vibrancy.id(name),
            FabricLoader.getInstance().getModContainer(Vibrancy.MOD_ID).orElseThrow(),
            Component.translatable("${Vibrancy.MOD_ID}.resource_pack.$name"),
            ResourcePackActivationType.DEFAULT_ENABLED
        )
    }
}