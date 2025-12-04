package net.typho.vibrancy.platform

import net.typho.vibrancy.Constants
import net.typho.vibrancy.platform.services.PlatformHelper
import java.util.*

object Services {
    val PLATFORM = load(PlatformHelper::class.java)

    fun <T> load(clazz: Class<T>): T {
        val loadedService = ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow {
                IllegalStateException("Failed to load service for ${clazz.name}")
            }
        Constants.LOGGER.debug("Loaded {} for service {}", loadedService, clazz)
        return loadedService
    }
}