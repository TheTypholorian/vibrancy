package net.typho.vibrancy.sky

import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.typho.big_shot_lib.api.util.resources.NeoFileToIdConverter
import net.typho.big_shot_lib.api.util.resources.NeoResourceManager
import net.typho.big_shot_lib.api.util.resources.NeoResourceManagerReloadListener
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.Vibrancy

object SkyLightInfoLoader : NeoResourceManagerReloadListener {
    @JvmField
    val idConverter = NeoFileToIdConverter.json("rtx/sky_lights")

    @JvmStatic
    fun load(key: ResourceIdentifier, json: JsonElement, file: ResourceIdentifier) {
        val typeKey = ResourceIdentifier.CODEC.decode(JsonOps.INSTANCE, json.asJsonObject.get("type"))
            .getOrThrow { JsonParseException("Error while parsing block light info $file: $it") }
            .first
        val codec = (SkyLightRegistry.registry!!.get(typeKey) ?: throw JsonParseException("No block light type $typeKey"))
                .infoCodec
        SkyLightRegistry.dimensionMap[key] = codec.codec()
            .parse(JsonOps.INSTANCE, json)
            .getOrThrow { message -> JsonParseException("Error parsing block light info for $key: $message") }
    }

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        SkyLightRegistry.dimensionMap.clear()

        for (entry in idConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                load(idConverter.fileToId(entry.key), JsonParser.parseReader(jsonReader), entry.key)
            }
        }

        Vibrancy.LOGGER.info("Loaded ${SkyLightRegistry.dimensionMap.size} block lights")
    }
}