package net.typho.vibrancy.sky

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.vibrancy.LightManager

abstract class ChunkedSkyLightStorage<I : SkyLightInfo, C : SkyLightStorage<I>>(val type: SkyLightType<I, *>) : SkyLightStorage<I> {
    @JvmField
    val chunks = HashMap<ChunkPos, C>()

    abstract fun createChunk(manager: LightManager, pos: ChunkPos): C

    fun getOrCreateChunk(manager: LightManager, pos: ChunkPos): C = chunks.computeIfAbsent(pos) { createChunk(manager, it) }

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
        chunks.remove(chunk.pos)?.deloadChunk(manager, chunk)
    }

    override fun clear(manager: LightManager) {
        chunks.values.forEach { it.clear(manager) }
        chunks.clear()
    }

    override fun reload(manager: LightManager) {
        chunks.values.forEach { it.reload(manager) }
    }
}