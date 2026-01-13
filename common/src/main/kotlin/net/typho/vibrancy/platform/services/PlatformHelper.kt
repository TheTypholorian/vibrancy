package net.typho.vibrancy.platform.services

import java.nio.file.Path

interface PlatformHelper {
    fun getPlatformName(): String

    fun isDevelopmentEnvironment(): Boolean

    fun getConfigDir(): Path

    fun registerResourcePack(name: String)
}