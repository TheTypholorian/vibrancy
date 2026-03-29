package net.typho.vibrancy.block.impl

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.HashMapBlockLightStorage

class RayPointLightStorage : HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>(RayPointLightType) {
    override fun createLight(
        manager: LightManager,
        state: BlockState,
        pos: AbstractVec3<Int>,
        info: RayPointLightInfo
    ) = if (info.enabled.apply(state)) RayPointLight(info, state, pos) else null

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        if (chunk == null) {
            map.values.forEach { it.reload() }
        } else {
            map.values.filter { ChunkPos(it.pos.blockPos) == chunk }
                .forEach { it.reload() }
        }
    }
}