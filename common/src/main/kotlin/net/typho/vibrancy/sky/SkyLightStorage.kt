package net.typho.vibrancy.sky

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager

interface SkyLightStorage<I> {
    val size: Int

    fun addLight(
        manager: LightManager,
        state: BlockState,
        pos: BlockPos,
        info: I
    )

    fun removeLight(
        manager: LightManager,
        pos: BlockPos
    )

    fun reload(manager: LightManager)

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