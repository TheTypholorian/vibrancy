package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager

abstract class ChunkedBlockLightStorage<I, C : BlockLightStorage<I>>(val type: BlockLightType<I, *>) : BlockLightStorage<I> {
    @JvmField
    val chunks = HashMap<ChunkPos, C>()
    override val size: Int
        get() = chunks.values.sumOf { it.size }

    abstract fun createChunk(pos: ChunkPos): C

    fun getOrCreateChunk(pos: ChunkPos): C = chunks.computeIfAbsent(pos, ::createChunk)

    override fun addLight(
        manager: LightManager,
        state: BlockState,
        pos: BlockPos,
        info: I
    ) {
        getOrCreateChunk(ChunkPos(pos)).addLight(manager, state, pos, info)
    }

    override fun removeLight(manager: LightManager, pos: BlockPos) {
        getOrCreateChunk(ChunkPos(pos)).removeLight(manager, pos)
    }

    override fun reload(manager: LightManager) {
        chunks.values.forEach { it.reload(manager) }
    }

    override fun loadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        getOrCreateChunk(chunk.pos).loadChunk(manager, chunk)
    }

    override fun deloadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        getOrCreateChunk(chunk.pos).deloadChunk(manager, chunk)
    }

    override fun clear(manager: LightManager) {
        chunks.values.forEach { it.clear(manager) }
    }
}