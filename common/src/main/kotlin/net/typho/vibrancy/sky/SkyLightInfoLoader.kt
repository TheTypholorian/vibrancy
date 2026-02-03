package net.typho.vibrancy.sky

import com.google.gson.JsonElement
import com.google.gson.JsonParser.parseReader
import com.google.gson.JsonSyntaxException
import com.mojang.serialization.JsonOps.INSTANCE
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.level.Level
import net.typho.vibrancy.Vibrancy

object SkyLightInfoLoader {
    @JvmField
    val idConverter: FileToIdConverter = FileToIdConverter.json("rtx/sky_lights")

    @JvmStatic
    fun load(dimension: Level, key: ResourceLocation?, json: JsonElement, registryAccess: RegistryAccess) {
        SkyLightRegistry.dimensionMap[dimension] = SkyLightRegistry.infoCodec(dimension, registryAccess)
            .codec()
            .parse(INSTANCE, json)
            .getOrThrow { message -> JsonSyntaxException("Error parsing sky light info for $key: $message") }
    }

    @JvmStatic
    fun load(manager: ResourceManager, registryAccess: RegistryAccess) {
        val dimensions = registryAccess.registryOrThrow(Registries.DIMENSION)

        SkyLightRegistry.dimensionMap.clear()

        for (entry in idConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val dimensionKey = idConverter.fileToId(entry.key)

                if (dimensions.containsKey(dimensionKey)) {
                    val dimension = dimensions.get(dimensionKey)
                    load(dimension!!, dimensionKey, parseReader(jsonReader), registryAccess)
                }
            }
        }

        Vibrancy.LOGGER.info("Loaded ${SkyLightRegistry.dimensionMap.size} sky lights")
    }
}