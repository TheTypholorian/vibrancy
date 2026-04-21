package net.typho.vibrancy.block

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.vibrancy.LightManager

abstract class ChunkedBlockLightStorage<I : BlockLightInfo, C : BlockLightStorage<I>>(val type: BlockLightType<I, *>) : BlockLightStorage<I> {
    @JvmField
    val chunks = HashMap<ChunkPos, C>()
    override val size: Int
        get() = chunks.values.sumOf { it.size }

    abstract fun createChunk(manager: LightManager, pos: ChunkPos): C

    fun getOrCreateChunk(manager: LightManager, pos: ChunkPos): C = chunks.computeIfAbsent(pos) { createChunk(manager, it) }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: I
    ) {
        getOrCreateChunk(manager, ChunkPos(pos.blockPos)).addLight(manager, level, state, pos, info)
    }

    override fun removeLight(manager: LightManager, level: Level, pos: IVec3<Int>): Boolean {
        return getOrCreateChunk(manager, ChunkPos(pos.blockPos)).removeLight(manager, level, pos)
    }

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        chunks.values.forEach { it.reload(manager, chunk) }
    }

    override fun loadChunk(
        manager: LightManager,
        chunk: ChunkAccess
    ) {
        getOrCreateChunk(manager, chunk.pos).loadChunk(manager, chunk)
    }

    override fun deloadChunk(
        manager: LightManager,
        chunk: ChunkAccess
    ) {
        getOrCreateChunk(manager, chunk.pos).deloadChunk(manager, chunk)
    }

    override fun clear(manager: LightManager) {
        chunks.values.forEach { it.clear(manager) }
    }
}