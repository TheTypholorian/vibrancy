package net.typho.vibrancy.block.impl

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.HashMapBlockLightStorage

class RayPointLightStorage : HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>(RayPointLightType) {
    override fun createLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: RayPointLightInfo
    ) = if (info.enabled(state)) RayPointLight(info, state, pos) else null

    override fun loadChunk(manager: LightManager, chunk: ChunkAccess) {
        for (light in map.values) {
            if (light.pos.x >= chunk.pos.minBlockX && light.pos.x <= chunk.pos.maxBlockX && light.pos.z >= chunk.pos.minBlockZ && light.pos.z <= chunk.pos.maxBlockZ) {
                light.reload()
            }
        }

        super.loadChunk(manager, chunk)
    }

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        synchronized(map) {
            if (chunk == null) {
                map.values.forEach { it.reload() }
            } else {
                map.values.filter { ChunkPos(it.pos.blockPos) == chunk }
                    .forEach { it.reload() }
            }
        }
    }
}