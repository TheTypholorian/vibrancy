package net.typho.vibrancy.platform.services

import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import java.nio.file.Path

interface PlatformHelper {
    fun getPlatformName(): String

    fun isDevelopmentEnvironment(): Boolean

    fun getConfigDir(): Path

    fun getRenderTypeTexture(renderType: RenderType): ResourceLocation
}