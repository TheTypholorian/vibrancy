package net.typho.vibrancy.platform

import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.PathPackResources
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.event.AddPackFindersEvent
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.platform.services.PlatformHelper
import java.nio.file.Path
import java.util.*

@EventBusSubscriber(modid = Vibrancy.MOD_ID)
class NeoForgePlatformHelper : PlatformHelper {
    override fun getPlatformName(): String {
        return "NeoForge"
    }

    override fun isDevelopmentEnvironment(): Boolean {
        return !FMLLoader.isProduction()
    }

    override fun getConfigDir(): Path = FMLPaths.CONFIGDIR.get()

    override fun registerResourcePack(name: String) {
        if (loadedPacks) {
            throw IllegalStateException("Cannot load built in resource pack $name after AddPackFindersEvent has run")
        }

        resourcePacks.add(name)
    }

    companion object {
        private val resourcePacks = LinkedList<String>()
        private var loadedPacks = false

        @SubscribeEvent
        @JvmStatic
        fun registerPacks(event: AddPackFindersEvent) {
            if (event.packType == PackType.CLIENT_RESOURCES) {
                loadedPacks = true

                val file = ModList.get().getModFileById(Vibrancy.MOD_ID).file

                for (pack in resourcePacks) {
                    Pack.readMetaAndCreate(
                        PackLocationInfo(
                            Vibrancy.MOD_ID + ":$pack",
                            Component.translatable("${Vibrancy.MOD_ID}.resource_pack.$pack"),
                            PackSource.BUILT_IN,
                            Optional.empty()
                        ),
                        PathPackResources.PathResourcesSupplier(file.findResource("resourcepacks/$pack")),
                        PackType.CLIENT_RESOURCES,
                        PackSelectionConfig(false, Pack.Position.TOP, false)
                    )?.let { pack ->
                        event.addRepositorySource { it.accept(pack) }
                    }
                }
            }
        }
    }
}