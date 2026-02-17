package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager

interface BlockLightStorage<I : BlockLightInfo<I, *>> {
    fun addLight(
        manager: LightManager,
        level: Level,
        state: StateHolder<*, *>,
        pos: BlockPos,
        info: I
    )

    fun removeLight(
        manager: LightManager,
        pos: BlockPos
    )

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

    fun size(): Int
}