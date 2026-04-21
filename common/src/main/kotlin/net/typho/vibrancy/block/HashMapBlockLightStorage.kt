package net.typho.vibrancy.block

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.vibrancy.LightManager
import org.lwjgl.system.NativeResource

abstract class HashMapBlockLightStorage<I : BlockLightInfo, L>(val type: BlockLightType<I, *>) : BlockLightStorage<I> {
    var map = HashMap<IVec3<Int>, L & Any>()
    override val size: Int
        get() = map.size

    abstract fun createLight(manager: LightManager, level: Level, state: BlockState, pos: IVec3<Int>, info: I): L?

    override fun addLight(manager: LightManager, level: Level, state: BlockState, pos: IVec3<Int>, info: I) {
        val light = createLight(manager, level, state, pos, info)

        synchronized(map) {
            if (light == null) {
                (map.remove(pos) as? NativeResource)?.free()
            } else {
                (map.put(pos, light) as? NativeResource)?.free()
            }
        }
    }

    override fun removeLight(manager: LightManager, level: Level, pos: IVec3<Int>): Boolean {
        synchronized(map) {
            val removed = map.remove(pos)
            (removed as? NativeResource)?.free()
            return removed != null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun loadChunk(manager: LightManager, chunk: ChunkAccess) {
        deloadChunk(manager, chunk)

        chunk.findBlocks(BlockLightRegistry::has) { pos, state ->
            val pos = NeoVec3i(pos)

            BlockLightRegistry.get(state.block, type)?.let { info ->
                type.castInfo(info)?.let {
                    addLight(
                        manager,
                        manager.getLevel()!!,
                        state,
                        pos,
                        it
                    )
                }
            }
        }
    }

    override fun deloadChunk(manager: LightManager, chunk: ChunkAccess) {
        synchronized(map) {
            map.entries.removeIf { entry ->
                val removed = ChunkPos(entry.key.blockPos) == chunk.pos

                if (removed) {
                    (entry.value as? NativeResource)?.free()
                }

                return@removeIf removed
            }
        }
    }

    override fun clear(manager: LightManager) {
        synchronized(map) {
            map.values.forEach { light -> (light as? NativeResource)?.free() }
            map.clear()
        }
    }
}