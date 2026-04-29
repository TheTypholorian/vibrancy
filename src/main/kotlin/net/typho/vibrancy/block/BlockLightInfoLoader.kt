package net.typho.vibrancy.block

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.typho.big_shot_lib.api.client.util.resource.NeoResourceManager
import net.typho.big_shot_lib.api.client.util.resource.NeoResourceManagerReloadListener
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.resource.NeoFileToIdConverter
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.big_shot_lib.api.util.resource.NeoTagKey
import net.typho.vibrancy.Vibrancy

object BlockLightInfoLoader : NeoResourceManagerReloadListener {
    override val location: NeoIdentifier = Vibrancy.id("block_lights")
    @JvmField
    val singleIdConverter = NeoFileToIdConverter.json("rtx/block_lights/by_block")
    @JvmField
    val tagIdConverter = NeoFileToIdConverter.json("rtx/block_lights/by_block_tag")
    private val warned = HashSet<NeoIdentifier>()

    @JvmStatic
    fun load(block: Block, key: NeoIdentifier, json: JsonObject) {
        val typeResult = NeoIdentifier.CODEC.decode(JsonOps.INSTANCE, json.get("type"))
        typeResult.error().ifPresent { throw JsonParseException("Block light type for $key is not a valid Identifier: $it") }
        val typeKey = typeResult.result().get().first

        val codec = (BlockLightRegistry.registry!!.get(typeKey) ?: return Vibrancy.LOGGER.error("No block light type $typeKey for $key"))
                .infoCodec(block.stateDefinition)
        val result = codec.codec().parse(JsonOps.INSTANCE, json)

        result.result().ifPresent { BlockLightRegistry.blockMap[block] = it }
        result.error().ifPresent { Vibrancy.LOGGER.error("Error parsing block light info for $key: ${it.message()}") }
    }

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        BlockLightRegistry.blockMap.clear()
        val blocks = WrapperUtil.INSTANCE.wrap(BuiltInRegistries.BLOCK)

        for (entry in tagIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                blocks.getTag(NeoTagKey(blocks.key.location, tagIdConverter.fileToId(entry.key)))?.let { tag ->
                    val json = JsonParser.parseReader(jsonReader).asJsonObject

                    if (!json.get("enabled").let { it != null && it.isJsonPrimitive && it.asJsonPrimitive.isBoolean && !it.asJsonPrimitive.asBoolean }) {
                        tag.forEach { block ->
                            load(block, blocks.getKey(block).location, json)
                        }
                    }
                }
            }
        }

        for (entry in singleIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = singleIdConverter.fileToId(entry.key)
                val block = blocks.get(blockKey)?.let { if (it == Blocks.AIR) null else it }

                if (block == null) {
                    if (warned.add(blockKey)) {
                        Vibrancy.LOGGER.warn("Couldn't find block $blockKey to give a block light to")
                    }
                } else {
                    val json = JsonParser.parseReader(jsonReader).asJsonObject

                    if (!json.get("enabled").let { it != null && it.isJsonPrimitive && it.asJsonPrimitive.isBoolean && !it.asJsonPrimitive.asBoolean }) {
                        load(block, blockKey, json)
                    }
                }
            }
        }

        Vibrancy.LOGGER.info("Loaded ${BlockLightRegistry.blockMap.size} block lights")
        Vibrancy.lightManager.reload()
    }
}