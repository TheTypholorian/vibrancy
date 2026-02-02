package net.typho.vibrancy.sky.impl

import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.sky.SkyLightStorage

class OverworldSkyLightStorage : SkyLightStorage<OverworldSkyLightInfo> {
    override fun rebuildShadows(manager: LightManager) {
        TODO("Not yet implemented")
    }

    override fun loadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        TODO("Not yet implemented")
    }

    override fun deloadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        TODO("Not yet implemented")
    }

    override fun clear(manager: LightManager) {
        TODO("Not yet implemented")
    }
}