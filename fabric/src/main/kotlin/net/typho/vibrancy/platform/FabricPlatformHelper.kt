package net.typho.vibrancy.platform

import foundry.veil.api.client.render.rendertype.VeilRenderType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
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

    override fun getRenderTypeTexture(renderType: RenderType): ResourceLocation {
        return when (renderType) {
            is RenderType.CompositeRenderType -> {
                renderType.state().textureState.cutoutTexture().orElseThrow()
            }
            is VeilRenderType.LayeredRenderType -> {
                getRenderTypeTexture(renderType.layers.first())
            }
            is VeilRenderType.RenderTypeWrapper -> {
                getRenderTypeTexture(renderType.get()!!)
            }
            else -> throw UnsupportedOperationException("Unable to get texture for render type ${renderType.javaClass} $renderType")
        }
    }
}