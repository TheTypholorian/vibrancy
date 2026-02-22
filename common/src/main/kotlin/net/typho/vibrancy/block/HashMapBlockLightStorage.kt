package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager

open class HashMapBlockLightStorage<I : BlockLightInfo<I, B>, B : BlockLight<I, B>>(val type: BlockLightType<I, B, *>) : BlockLightStorage<I> {
    val map = HashMap<BlockPos, B>()

    override fun addLight(manager: LightManager, level: Level, state: BlockState, pos: BlockPos, info: I) {
        val light = info.createBlockLight(manager, level, state, pos)

        if (light == null) {
            map.remove(pos)?.free(manager)
        } else {
            map.put(pos, light)?.free(manager)
        }
    }

    override fun removeLight(manager: LightManager, pos: BlockPos) {
        map.remove(pos)?.free(manager)
    }

    override fun rebuildShadows(manager: LightManager) {
        map.values.forEach { light -> light.rebuildShadows(manager) }
    }

    override fun resizeShadows(manager: LightManager) {
        map.values.forEach { light -> light.resizeShadows(manager) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun loadChunk(manager: LightManager, chunk: LevelChunk) {
        deloadChunk(manager, chunk)

        chunk.findBlocks({ BlockLightRegistry.has(it.block) }) { pos, state ->
            val actualPos = BlockPos(pos)

            BlockLightRegistry.get(state.block)?.let { info ->
                if (info.type() == type) {
                    addLight(
                        manager,
                        chunk.level!!,
                        state,
                        actualPos,
                        info as I
                    )
                }
            }
        }
    }

    override fun deloadChunk(manager: LightManager, chunk: LevelChunk) {
        map.entries.removeIf { entry ->
            val removed = ChunkPos(entry.key) == chunk.pos

            if (removed) {
                entry.value.free(manager)
            }

            return@removeIf removed
        }
    }

    override fun clear(manager: LightManager) {
        map.values.forEach { light -> light.free(manager) }
        map.clear()
    }

    override fun size(): Int {
        return map.size
    }
}