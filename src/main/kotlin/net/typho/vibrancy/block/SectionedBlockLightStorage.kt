package net.typho.vibrancy.block

import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.vibrancy.LightManager
import org.lwjgl.system.NativeResource

abstract class SectionedBlockLightStorage<I : BlockLightInfo, C : BlockLightStorage<I>>(val type: BlockLightType<I, *>) : BlockLightStorage<I> {
    @JvmField
    val chunks = HashMap<SectionPos, C>()
    override val size: Int
        get() = chunks.values.sumOf { it.size }

    override fun shouldCollectMeshGeometry(pos: SectionPos): Boolean {
        return chunks.containsKey(pos)
    }

    abstract fun createChunk(manager: LightManager, pos: SectionPos): C

    fun getOrCreateChunk(manager: LightManager, pos: SectionPos): C = chunks.computeIfAbsent(pos) { createChunk(manager, it) }

    fun getOrLoadChunk(manager: LightManager, pos: SectionPos): C = chunks.computeIfAbsent(pos) { createChunk(manager, it).also { it.loadChunk(manager, manager.getLevel()!!.getChunk(pos.x, pos.z) ) } }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: I
    ) {
        getOrLoadChunk(manager, SectionPos.of(pos.blockPos)).addLight(manager, level, state, pos, info)
    }

    override fun removeLight(manager: LightManager, level: Level, pos: IVec3<Int>): Boolean {
        return getOrLoadChunk(manager, SectionPos.of(pos.blockPos)).removeLight(manager, level, pos)
    }

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        chunks.values.forEach { it.reload(manager, chunk) }
    }

    override fun loadChunk(
        manager: LightManager,
        chunk: ChunkAccess
    ) {
        for (i in chunk.minSection until chunk.maxSection) {
            loadSection(manager, chunk, SectionPos.of(chunk.pos, i))
        }
    }

    abstract fun loadSection(manager: LightManager, chunk: ChunkAccess, pos: SectionPos)

    override fun deloadChunk(
        manager: LightManager,
        chunk: ChunkAccess
    ) {
        for (i in chunk.minSection until chunk.maxSection) {
            val pos = SectionPos.of(chunk.pos, i)
            chunks[pos]?.let {
                it.deloadChunk(manager, chunk)
                (it as? NativeResource)?.free()
                chunks.remove(pos)
            }
        }
    }

    override fun clear(manager: LightManager) {
        chunks.values.forEach { it.clear(manager) }
    }
}