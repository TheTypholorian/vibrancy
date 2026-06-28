package net.typho.vibrancy.block

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.event.NeoEventBus
import net.typho.big_shot_lib.api.event.NewRegistryEvent
import net.typho.big_shot_lib.api.event.RegisterEvent
import net.typho.big_shot_lib.api.event.RegistryBuilder
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLightType
import net.typho.vibrancy.block.impl.SubtleLightType

object BlockLightRegistry {
    @JvmField
    val registryKey: ResourceKey<Registry<BlockLightType<*, *>>> =
        ResourceKey.createRegistryKey(Vibrancy.id("block_light_types"))
    lateinit var registry: Registry<BlockLightType<*, *>>

    @JvmField
    val blockMap = HashMap<Block, BlockLightInfo>()

    @JvmStatic
    fun <I : BlockLightInfo> get(block: Block, type: BlockLightType<I, *>): I? = type.castInfo(blockMap[block])

    @JvmStatic
    fun has(block: Block): Boolean = blockMap.containsKey(block)

    @JvmStatic
    fun has(state: BlockState): Boolean = blockMap[state.block]?.enabled?.invoke(state) ?: false

    @JvmStatic
    fun onInitialize(bus: NeoEventBus) {
        bus.register(NewRegistryEvent { output ->
            registry = output.register(RegistryBuilder(registryKey))
        })
        bus.register(RegisterEvent { output ->
            output.begin(registryKey) {
                it.register(Vibrancy.id("raytraced_point"), RayPointLightType)
                it.register(Vibrancy.id("subtle"), SubtleLightType)
            }
        })
    }
}