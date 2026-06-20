package net.typho.vibrancy.block

import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.LightManager

interface BlockLightStorage<I> {
    val size: Int

    fun shouldCollectMeshGeometry(pos: SectionPos): Boolean

    fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: I
    )

    fun removeLight(
        manager: LightManager,
        level: Level,
        pos: IVec3<Int>
    ): Boolean

    fun reload(manager: LightManager, chunk: ChunkPos?)

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