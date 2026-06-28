package net.typho.vibrancy.block.impl

import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.HashMapBlockLightStorage

class RayPointLightStorage : HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>(RayPointLightType) {
    var dirty = false

    override fun shouldCollectMeshGeometry(pos: SectionPos): Boolean {
        synchronized(map) {
            return map.values.any { it.sections.contains(pos) }
        }
    }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: RayPointLightInfo
    ) {
        super.addLight(manager, level, state, pos, info)
        dirty = true
    }

    override fun removeLight(manager: LightManager, level: Level, pos: IVec3<Int>): Boolean {
        val r = super.removeLight(manager, level, pos)
        dirty = dirty or r
        return r
    }

    override fun loadChunk(manager: LightManager, chunk: ChunkAccess) {
        super.loadChunk(manager, chunk)
        dirty = true
    }

    override fun deloadChunk(manager: LightManager, chunk: ChunkAccess) {
        super.deloadChunk(manager, chunk)
        dirty = true
    }

    override fun clear(manager: LightManager) {
        super.clear(manager)
        dirty = true
    }

    override fun createLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: RayPointLightInfo
    ) = if (info.enabled(state)) RayPointLight(level, info, state, pos) else null

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        synchronized(map) {
            if (chunk == null) {
                map.values.forEach { it.reload() }
            } else {
                map.values.filter { ChunkPos.containing(it.pos.toBlockPos()) == chunk }
                    .forEach { it.reload() }
            }
        }
    }

    override fun endFrame(manager: LightManager) {
        dirty = false
    }
}