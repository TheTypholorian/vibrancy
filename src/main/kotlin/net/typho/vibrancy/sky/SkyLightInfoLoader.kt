package net.typho.vibrancy.sky

import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.typho.big_shot_lib.api.client.util.resource.NeoResourceManager
import net.typho.big_shot_lib.api.client.util.resource.NeoResourceManagerReloadListener
import net.typho.big_shot_lib.api.util.resource.NeoFileToIdConverter
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.Vibrancy

object SkyLightInfoLoader : NeoResourceManagerReloadListener {
    override val location: NeoIdentifier = Vibrancy.id("sky_lights")
    @JvmField
    val idConverter = NeoFileToIdConverter.json("rtx/sky_lights")

    @JvmStatic
    fun load(key: NeoIdentifier, json: JsonElement, file: NeoIdentifier) {
        val typeResult = NeoIdentifier.CODEC.decode(JsonOps.INSTANCE, json.asJsonObject.get("type"))
        typeResult.error().ifPresent { throw JsonParseException("Sky light type for $key is not a valid Identifier: $it") }
        val typeKey = typeResult.result().get().first

        val codec = (SkyLightRegistry.registry!!.get(typeKey) ?: throw JsonParseException("No sky light type $typeKey"))
                .infoCodec
        val result = codec.codec().parse(JsonOps.INSTANCE, json)

        result.result().ifPresent { SkyLightRegistry.dimensionMap[key] = it }
        result.error().ifPresent { Vibrancy.LOGGER.error("Error parsing sky light info for $key: ${it.message()}") }
    }

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        SkyLightRegistry.dimensionMap.clear()

        for (entry in idConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                load(idConverter.fileToId(entry.key), JsonParser.parseReader(jsonReader), entry.key)
            }
        }

        Vibrancy.LOGGER.info("Loaded ${SkyLightRegistry.dimensionMap.size} sky lights")
    }
}