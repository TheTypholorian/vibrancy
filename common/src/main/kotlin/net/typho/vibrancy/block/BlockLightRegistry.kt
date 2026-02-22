package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.registration.RegistrationFactory
import net.typho.big_shot_lib.api.util.NeoRegistry
import net.typho.big_shot_lib.api.util.resources.NeoResourceKey
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLightType
import net.typho.vibrancy.block.impl.SubtleLightType

object BlockLightRegistry {
    @JvmField
    val registryKey: NeoResourceKey<Registry<BlockLightType<*, *, *>>> =
        NeoResourceKey.registry(Vibrancy.id("block_light_types"))
    @JvmField
    var registry: NeoRegistry<BlockLightType<*, *, *>>? = null

    @JvmField
    val blockMap = HashMap<Block, BlockLightInfo<*, *>>()

    @JvmStatic
    fun get(block: Block): BlockLightInfo<*, *>? = blockMap[block]

    @JvmStatic
    fun has(block: Block): Boolean = blockMap.containsKey(block)

    @JvmStatic
    fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<BlockLightInfo<*, *>> {
        return NeoResourceKey.codec<BlockLightType<*, *, *>>(registryKey.location).dispatchMap(
            { info -> registry!!.getKey(info.type()) },
            { key -> registry!!.get(key)!!.infoCodec(stateDefinition) }
        )
    }

    @JvmStatic
    fun registerBuiltins(factory: RegistrationFactory) {
        val consumer = factory.begin(registryKey, Vibrancy.MOD_ID)
        consumer.register("raytraced_point") { RayPointLightType }
        consumer.register("subtle") { SubtleLightType }
    }
}