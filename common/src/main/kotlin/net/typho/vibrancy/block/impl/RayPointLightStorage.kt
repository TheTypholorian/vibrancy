package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.StateHolder
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.HashMapBlockLightStorage

class RayPointLightStorage : HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>(RayPointLightType) {
    override fun createLight(
        manager: LightManager,
        state: StateHolder<*, *>,
        pos: BlockPos,
        info: RayPointLightInfo
    ): RayPointLight {
        return RayPointLight(info, state, pos)
    }
}