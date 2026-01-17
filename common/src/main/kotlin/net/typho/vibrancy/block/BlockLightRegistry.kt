package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.Vibrancy

object BlockLightRegistry {
    @JvmField
    val registryKey: ResourceKey<Registry<BlockLightType<*, *, *>>> =
        ResourceKey.createRegistryKey(Vibrancy.id("block_light_types"))

    @JvmField
    val blockMap = HashMap<Block, BlockLightInfo<*, *>>()

    @JvmStatic
    fun get(block: Block): BlockLightInfo<*, *>? = blockMap[block]

    @JvmStatic
    fun has(block: Block): Boolean = blockMap.containsKey(block)

    @JvmStatic
    fun infoCodec(stateDefinition: StateDefinition<*, *>, registryAccess: RegistryAccess): MapCodec<BlockLightInfo<*, *>> {
        val types = registryAccess.registryOrThrow(registryKey)
        return ResourceKey.codec(registryKey).dispatchMap(
            { info -> types.getResourceKey(info.type()).orElseThrow() },
            { key -> types.get(key)!!.infoCodec(stateDefinition) }
        )
    }

    fun init() = Unit
}