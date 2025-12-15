package net.typho.vibrancy.platform

import net.fabricmc.loader.api.FabricLoader
import net.irisshaders.batchedentityrendering.impl.wrappers.TaggingRenderTypeWrapper
import net.irisshaders.iris.layer.InnerWrappedRenderType
import net.irisshaders.iris.layer.OuterWrappedRenderType
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
            is OuterWrappedRenderType -> {
                getRenderTypeTexture(renderType.unwrap())
            }
            is InnerWrappedRenderType -> {
                getRenderTypeTexture(renderType.unwrap())
            }
            is TaggingRenderTypeWrapper -> {
                getRenderTypeTexture(renderType.unwrap())
            }
            else -> throw UnsupportedOperationException("Unable to get texture for render type ${renderType.javaClass} $renderType")
        }
    }
}