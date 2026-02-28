package net.typho.vibrancy.sky

import com.google.gson.JsonElement
import com.google.gson.JsonParser.parseReader
import com.google.gson.JsonSyntaxException
import com.mojang.serialization.JsonOps.INSTANCE
import net.typho.big_shot_lib.api.util.resources.NeoFileToIdConverter
import net.typho.big_shot_lib.api.util.resources.NeoResourceManager
import net.typho.big_shot_lib.api.util.resources.NeoResourceManagerReloadListener
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.Vibrancy

object SkyLightInfoLoader : NeoResourceManagerReloadListener {
    @JvmField
    val idConverter = NeoFileToIdConverter.json("rtx/sky_lights")

    @JvmStatic
    fun load(key: ResourceIdentifier, json: JsonElement) {
        SkyLightRegistry.dimensionMap[key] = SkyLightRegistry.infoCodec(key)
            .codec()
            .parse(INSTANCE, json)
            .getOrThrow { message -> JsonSyntaxException("Error parsing sky light info for $key: $message") }
    }

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        SkyLightRegistry.dimensionMap.clear()

        for (entry in idConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val dimensionKey = idConverter.fileToId(entry.key)

                load(dimensionKey, parseReader(jsonReader))
            }
        }

        Vibrancy.LOGGER.info("Loaded ${SkyLightRegistry.dimensionMap.size} sky lights")
    }
}