package net.typho.vibrancy.sky

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager

abstract class ChunkedSkyLightStorage<I : SkyLightInfo, C : SkyLightStorage<I>>(val type: SkyLightType<I, *>) : SkyLightStorage<I> {
    @JvmField
    val chunks = HashMap<ChunkPos, C>()

    abstract fun createChunk(manager: LightManager, level: Level, pos: ChunkPos): C

    fun getOrCreateChunk(manager: LightManager, level: Level, pos: ChunkPos): C = chunks.computeIfAbsent(pos) { createChunk(manager, level, it) }

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

    override fun reload(manager: LightManager) {
        chunks.values.forEach { it.reload(manager) }
    }
}