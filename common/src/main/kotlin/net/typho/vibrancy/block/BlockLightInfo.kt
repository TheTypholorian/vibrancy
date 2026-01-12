package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.StateHolder
import net.typho.vibrancy.LightManager

interface BlockLightInfo {
    fun type(): BlockLightType<*, *>

    fun createBlockLight(
        state: StateHolder<*, *>,
        pos: BlockPos
    ): BlockLight<*>?

    fun addBlockLight(
        manager: LightManager,
        state: StateHolder<*, *>,
        pos: BlockPos
    ): Boolean {
        val new = manager.blockLights.compute(pos) { pos1, old ->
            old?.free()
            createBlockLight(state, pos)
        }
        return new != null
    }
}