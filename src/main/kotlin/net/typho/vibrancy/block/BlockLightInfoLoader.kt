package net.typho.vibrancy.block

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.typho.big_shot_lib.api.util.resource.SingleStepNeoReloadListener
import net.typho.vibrancy.Vibrancy
import kotlin.jvm.optionals.getOrNull

object BlockLightInfoLoader : SingleStepNeoReloadListener {
    override val location: Identifier = Vibrancy.id("block_lights")
    @JvmField
    val singleIdConverter = FileToIdConverter.json("rtx/block_lights/by_block")
    @JvmField
    val tagIdConverter = FileToIdConverter.json("rtx/block_lights/by_block_tag")
    private val warned = HashSet<Identifier>()

    @JvmStatic
    fun load(block: Holder<Block>, json: JsonObject) {
        val typeResult = Identifier.CODEC.decode(JsonOps.INSTANCE, json.get("type"))
        typeResult.error().ifPresent { throw JsonParseException("Block light type for ${block.unwrapKey().getOrNull()?.identifier()} is not a valid Identifier: $it") }
        val typeKey = typeResult.result().get().first

        val codec = (BlockLightRegistry.registry.get(typeKey).getOrNull() ?: return Vibrancy.LOGGER.error("No block light type $typeKey for ${block.unwrapKey().getOrNull()?.identifier()}"))
            .value()
            .infoCodec(block.value().stateDefinition)
        val result = codec.codec().parse(JsonOps.INSTANCE, json)

        result.result().ifPresent { BlockLightRegistry.blockMap[block.value()] = it }
        result.error().ifPresent { Vibrancy.LOGGER.error("Error parsing block light info for ${block.unwrapKey().getOrNull()?.identifier()}: ${it.message()}") }
    }

    override fun onResourceManagerReload(manager: ResourceManager) {
        BlockLightRegistry.blockMap.clear()
        val blocks = BuiltInRegistries.BLOCK

        for (entry in tagIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val json = JsonParser.parseReader(jsonReader).asJsonObject

                if (!json.get("enabled").let { it != null && it.isJsonPrimitive && it.asJsonPrimitive.isBoolean && !it.asJsonPrimitive.asBoolean }) {
                    blocks.getTagOrEmpty(TagKey.create(Registries.BLOCK, tagIdConverter.fileToId(entry.key))).forEach { block ->
                        load(block, json)
                    }
                }
            }
        }

        for (entry in singleIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = singleIdConverter.fileToId(entry.key)
                val block = blocks.get(blockKey).getOrNull()

                if (block == null) {
                    if (warned.add(blockKey)) {
                        Vibrancy.LOGGER.warn("Couldn't find block $blockKey to give a block light to")
                    }
                } else {
                    val json = JsonParser.parseReader(jsonReader).asJsonObject

                    if (!json.get("enabled").let { it != null && it.isJsonPrimitive && it.asJsonPrimitive.isBoolean && !it.asJsonPrimitive.asBoolean }) {
                        load(block, json)
                    }
                }
            }
        }

        Vibrancy.LOGGER.info("Loaded ${BlockLightRegistry.blockMap.size} block lights")
        Vibrancy.lightManager.reload()
    }
}