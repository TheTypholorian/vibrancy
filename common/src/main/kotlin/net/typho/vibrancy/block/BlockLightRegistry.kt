package net.typho.vibrancy.block

import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.util.NeoRegistry
import net.typho.big_shot_lib.api.util.RegistrationFactory
import net.typho.big_shot_lib.api.util.resource.NeoResourceKey
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLightType
import net.typho.vibrancy.block.impl.SubtleLightType

object BlockLightRegistry {
    @JvmField
    val registryKey: NeoResourceKey<Registry<BlockLightType<*, *>>> =
        NeoResourceKey.registry(Vibrancy.id("block_light_types"))
    @JvmField
    var registry: NeoRegistry<BlockLightType<*, *>>? = null

    @JvmField
    val blockMap = HashMap<Block, BlockLightInfo>()

    @JvmStatic
    fun <I : BlockLightInfo> get(block: Block, type: BlockLightType<I, *>): I? = type.castInfo(blockMap[block])

    @JvmStatic
    fun has(block: Block): Boolean = blockMap.containsKey(block)

    @JvmStatic
    fun has(state: BlockState): Boolean = blockMap[state.block]?.enabled?.invoke(state) ?: false

    @JvmStatic
    fun registerBuiltins(factory: RegistrationFactory) {
        factory.begin(registryKey, Vibrancy.MOD_ID)?.run {
            register("raytraced_point") { RayPointLightType }
            register("subtle") { SubtleLightType }
        }
    }
}