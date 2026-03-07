package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager

abstract class ChunkedBlockLightStorage<I : BlockLightInfo, C : BlockLightStorage<I>>(val type: BlockLightType<I, *>) : BlockLightStorage<I> {
    @JvmField
    val chunks = HashMap<ChunkPos, C>()
    override val size: Int
        get() = chunks.values.sumOf { it.size }

    abstract fun createChunk(manager: LightManager, level: Level, pos: ChunkPos): C

    fun getOrCreateChunk(manager: LightManager, level: Level, pos: ChunkPos): C = chunks.computeIfAbsent(pos) { createChunk(manager, level, it) }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: BlockPos,
        info: I
    ) {
        getOrCreateChunk(manager, level, ChunkPos(pos)).addLight(manager, level, state, pos, info)
    }

    override fun removeLight(manager: LightManager, level: Level, pos: BlockPos): Boolean {
        return getOrCreateChunk(manager, level, ChunkPos(pos)).removeLight(manager, level, pos)
    }

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        chunks.values.forEach { it.reload(manager, chunk) }
    }

    override fun loadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        getOrCreateChunk(manager, chunk.level!!, chunk.pos).loadChunk(manager, chunk)
    }

    override fun deloadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        getOrCreateChunk(manager, chunk.level!!, chunk.pos).deloadChunk(manager, chunk)
    }

    override fun clear(manager: LightManager) {
        chunks.values.forEach { it.clear(manager) }
    }
}