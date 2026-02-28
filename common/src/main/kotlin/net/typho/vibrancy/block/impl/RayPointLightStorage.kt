package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.HashMapBlockLightStorage

class RayPointLightStorage : HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>(RayPointLightType) {
    override fun createLight(
        manager: LightManager,
        state: BlockState,
        pos: BlockPos,
        info: RayPointLightInfo
    ) = RayPointLight(info, state, pos)

    override fun reload(manager: LightManager) {
        map.values.forEach { it.reload(manager) }
    }
}