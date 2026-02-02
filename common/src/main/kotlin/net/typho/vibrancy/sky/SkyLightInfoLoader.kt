package net.typho.vibrancy.sky

import com.google.gson.JsonElement
import com.google.gson.JsonParser.parseReader
import com.google.gson.JsonSyntaxException
import com.mojang.serialization.JsonOps.INSTANCE
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.level.block.Block
import net.typho.big_shot_lib.resource.SynchronousReloadListener
import net.typho.vibrancy.Vibrancy

object SkyLightInfoLoader : SynchronousReloadListener {
    @JvmField
    val idConverter: FileToIdConverter = FileToIdConverter.json("rtx/sky_lights")

    @JvmStatic
    fun load(block: Block, key: ResourceLocation?, json: JsonElement) {
        SkyLightRegistry.dimensionMap[block] = SkyLightRegistry.infoCodec(block.stateDefinition)
            .codec()
            .parse(INSTANCE, json)
            .getOrThrow { message -> JsonSyntaxException("Error parsing block light info for $key: $message") }
    }

    override fun reload(manager: ResourceManager) {
        SkyLightRegistry.dimensionMap.clear()

        for (entry in idConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = idConverter.fileToId(entry.key)

                if (BuiltInRegistries..containsKey(blockKey)) {
                    val block = BuiltInRegistries.BLOCK.get(blockKey)
                    load(block, blockKey, parseReader(jsonReader))
                }
            }
        }

        Vibrancy.LOGGER.info("Loaded ${SkyLightRegistry.dimensionMap.size} block lights")
    }
}