package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager

interface BlockLightStorage<I> {
    val size: Int

    fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: BlockPos,
        info: I
    )

    fun removeLight(
        manager: LightManager,
        level: Level,
        pos: BlockPos
    ): Boolean

    fun reload(manager: LightManager, chunk: ChunkPos?)

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