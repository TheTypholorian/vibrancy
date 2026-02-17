package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.util.NeoRegistryAccess
import net.typho.big_shot_lib.api.util.resources.NeoResourceKey
import net.typho.vibrancy.Vibrancy

object BlockLightRegistry {
    @JvmField
    val registryKey: NeoResourceKey<Registry<BlockLightType<*, *, *>>> =
        NeoResourceKey.registry(Vibrancy.id("block_light_types"))

    @JvmField
    val blockMap = HashMap<Block, BlockLightInfo<*, *>>()

    @JvmStatic
    fun get(block: Block): BlockLightInfo<*, *>? = blockMap[block]

    @JvmStatic
    fun has(block: Block): Boolean = blockMap.containsKey(block)

    @JvmStatic
    fun infoCodec(stateDefinition: StateDefinition<*, *>, registryAccess: NeoRegistryAccess): MapCodec<BlockLightInfo<*, *>> {
        val types = registryAccess.registry(registryKey)!!
        return NeoResourceKey.codec<BlockLightType<*, *, *>>(registryKey.location).dispatchMap(
            { info -> types.getKey(info.type()) },
            { key -> types.get(key)!!.infoCodec(stateDefinition) }
        )
    }

    fun init() = Unit
}