package net.typho.vibrancy.sky

import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.vibrancy.LightManager

interface SkyLightStorage<I : SkyLightInfo> {
    fun load(manager: LightManager, info: I)

    fun reload(manager: LightManager)

    fun loadChunk(
        manager: LightManager,
        chunk: ChunkAccess
    )

    fun deloadChunk(
        manager: LightManager,
        chunk: ChunkAccess
    )

    fun clear(manager: LightManager)
}