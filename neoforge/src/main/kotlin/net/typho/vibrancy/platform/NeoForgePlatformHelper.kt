package net.typho.vibrancy.platform

import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.FMLPaths
import net.typho.vibrancy.platform.services.PlatformHelper
import java.nio.file.Path

class NeoForgePlatformHelper : PlatformHelper {
    override fun getPlatformName(): String {
        return "NeoForge"
    }

    override fun isDevelopmentEnvironment(): Boolean {
        return !FMLLoader.isProduction()
    }

    override fun getConfigDir(): Path = FMLPaths.CONFIGDIR.get()

    override fun getRenderTypeTexture(renderType: RenderType): ResourceLocation {
        return when (renderType) {
            is RenderType.CompositeRenderType -> {
                renderType.state().textureState.cutoutTexture().orElseThrow()
            }
            else -> throw UnsupportedOperationException("Unable to get texture for render type ${renderType.javaClass} $renderType")
        }
    }
}