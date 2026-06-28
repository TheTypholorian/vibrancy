package net.typho.vibrancy.sky

import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.Registries
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.util.resource.SingleStepNeoReloadListener
import net.typho.vibrancy.Vibrancy
import kotlin.jvm.optionals.getOrNull

object SkyLightInfoLoader : SingleStepNeoReloadListener {
    override val location: Identifier = Vibrancy.id("sky_lights")
    @JvmField
    val idConverter = FileToIdConverter.json("rtx/sky_lights")

    @JvmStatic
    fun load(key: ResourceKey<Level>, json: JsonElement) {
        val typeResult = Identifier.CODEC.decode(JsonOps.INSTANCE, json.asJsonObject.get("type"))
        typeResult.error().ifPresent { throw JsonParseException("Sky light type for ${key.identifier()} is not a valid Identifier: $it") }
        val typeKey = typeResult.result().get().first

        val codec = (SkyLightRegistry.registry.get(typeKey).getOrNull() ?: throw JsonParseException("No sky light type $typeKey"))
            .value()
            .infoCodec
        val result = codec.codec().parse(JsonOps.INSTANCE, json)

        result.result().ifPresent { SkyLightRegistry.dimensionMap[key] = it }
        result.error().ifPresent { Vibrancy.LOGGER.error("Error parsing sky light info for ${key.identifier()}: ${it.message()}") }
    }

    override fun onResourceManagerReload(manager: ResourceManager) {
        SkyLightRegistry.dimensionMap.clear()

        for (entry in idConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                load(ResourceKey.create(Registries.DIMENSION, idConverter.fileToId(entry.key)), JsonParser.parseReader(jsonReader))
            }
        }

        Vibrancy.LOGGER.info("Loaded ${SkyLightRegistry.dimensionMap.size} sky lights")
    }
}