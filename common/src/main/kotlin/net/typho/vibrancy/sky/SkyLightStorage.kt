package net.typho.vibrancy.sky

import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager

interface SkyLightStorage<I : SkyLightInfo<I, *>> {
    fun rebuildShadows(manager: LightManager)

    fun loadChunk(
        manager: LightManager,
        chunk: LevelChunk
    )

    fun deloadChunk(
        manager: LightManager,
        chunk: LevelChunk
    )

    fun clear(manager: LightManager)
}