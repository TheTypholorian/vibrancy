package net.typho.vibrancy.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager
import org.lwjgl.system.NativeResource

abstract class HashMapBlockLightStorage<I, L>(val type: BlockLightType<I, *>) : BlockLightStorage<I> {
    val map = HashMap<BlockPos, L>()
    override val size: Int
        get() = map.size

    abstract fun createLight(manager: LightManager, state: BlockState, pos: BlockPos, info: I): L?

    override fun addLight(manager: LightManager, state: BlockState, pos: BlockPos, info: I) {
        //println("Add light $state at $pos for $type")
        val light = createLight(manager, state, pos, info)

        if (light == null) {
            (map.remove(pos) as? NativeResource)?.free()
        } else {
            (map.put(pos, light) as? NativeResource)?.free()
        }
    }

    override fun removeLight(manager: LightManager, pos: BlockPos) {
        //println("Remove light at $pos for $type")
        (map.remove(pos) as? NativeResource)?.free()
    }

    @Suppress("UNCHECKED_CAST")
    override fun loadChunk(manager: LightManager, chunk: LevelChunk) {
        deloadChunk(manager, chunk)
        //println("Load chunk ${chunk.pos} for $type")

        chunk.findBlocks({ BlockLightRegistry.has(it.block) }) { pos, state ->
            val actualPos = BlockPos(pos)

            BlockLightRegistry.get(state.block, type)?.let { info ->
                type.castInfo(info)?.let {
                    addLight(
                        manager,
                        state,
                        actualPos,
                        it
                    )
                }
            }
        }
    }

    override fun deloadChunk(manager: LightManager, chunk: LevelChunk) {
        //println("Deload chunk ${chunk.pos} for $type")
        map.entries.removeIf { entry ->
            val removed = ChunkPos(entry.key) == chunk.pos

            if (removed) {
                (entry.value as? NativeResource)?.free()
            }

            return@removeIf removed
        }
    }

    override fun clear(manager: LightManager) {
        //println("Clear $type")
        map.values.forEach { light -> (light as? NativeResource)?.free() }
        map.clear()
    }
}