package net.typho.vibrancy.block

import com.mojang.serialization.Lifecycle
import com.mojang.serialization.MapCodec
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLightType
import net.typho.vibrancy.block.impl.SubtleLight
import net.typho.vibrancy.block.impl.SubtleLightType

object BlockLightRegistry {
    @JvmField
    val typesKey: ResourceKey<Registry<BlockLightType<*, *>>> =
        ResourceKey.createRegistryKey(Vibrancy.id("block_light_types"))
    @JvmField
    @Suppress("UNCHECKED_CAST")
    val types: Registry<BlockLightType<*, *>> = Registry.register(
        BuiltInRegistries.REGISTRY as Registry<Registry<BlockLightType<*, *>>>,
        typesKey,
        MappedRegistry(typesKey, Lifecycle.stable())
    )

    @JvmField
    val raytracedPoint: RayPointLightType = Registry.register(
        types,
        Vibrancy.id("raytraced_point"),
        RayPointLightType
    )
    @JvmField
    val subtle: SubtleLightType = Registry.register(
        types,
        Vibrancy.id("subtle"),
        SubtleLightType
    )

    @JvmField
    val blockMap = HashMap<Block, BlockLightInfo>()

    @JvmStatic
    fun get(block: Block): BlockLightInfo? = blockMap[block]

    @JvmStatic
    fun has(block: Block): Boolean = blockMap.containsKey(block)

    @JvmStatic
    fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<BlockLightInfo> {
        return ResourceKey.codec(typesKey).dispatchMap(
            { info -> types.getResourceKey(info.type()).orElseThrow() },
            { key -> types.get(key)!!.codec(stateDefinition) }
        )
    }

    fun init() = Unit
}