package net.typho.vibrancy.platform.services

interface PlatformHelper {
    fun getPlatformName(): String

    fun isDevelopmentEnvironment(): Boolean
}