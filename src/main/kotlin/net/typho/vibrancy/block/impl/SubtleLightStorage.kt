package net.typho.vibrancy.block.impl

import net.minecraft.core.Direction
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.core.SectionPos
import net.typho.big_shot_lib.api.math.IRect3
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.block.SectionedBlockLightStorage
import net.typho.vibrancy.util.GpuTask
import org.lwjgl.system.NativeResource

//? if sable {
/*import dev.ryanhcode.sable.companion.SableCompanion
import net.typho.vibrancy.Vibrancy
import org.joml.Quaternionf
*///? }

class SubtleLightStorage : SectionedBlockLightStorage<SubtleLightInfo, SubtleLightStorage.Chunk>(SubtleLightType) {
    companion object {
        /*
        @JvmField
        val VERTEX_FORMAT = VertexFormat.builder(0) // TODO
            .add("Position", VertexFormatElement.POSITION) // 12 bytes
            .add("UV0", LightMesh.COMPACT_TEXTURE_UV) // 4 bytes
            .add("LightIndex", LightMesh.LIGHT_INDEX) // 2 bytes
            .add("Color", VertexFormatElement.COLOR) // 4 bytes
            .add("Normal", VertexFormatElement.NORMAL) // 3 bytes
            .build(Vibrancy.id("subtle_mesh"))
         */
    }

    @JvmField
    val tasks = hashMapOf<SectionPos, GpuTask<Unit>>()
    @JvmField
    val scannedQueue = hashSetOf<SectionPos>()
    @JvmField
    val scannedRemoveQueue = hashSetOf<ChunkPos>()
    @JvmField
    val scanned = hashSetOf<SectionPos>()
    @JvmField
    val sectionLoadQueue = hashSetOf<SectionPos>()
    @JvmField
    val sectionLoadTasks = hashSetOf<GpuTask<Unit>>()

    override fun createChunk(manager: LightManager, pos: SectionPos): Chunk {
        return Chunk(pos)
    }

    override fun loadSection(manager: LightManager, chunk: ChunkAccess, pos: SectionPos) {
        val section = chunk.getSection(chunk.getSectionIndexFromSectionY(pos.y))

        if (section.maybeHas { BlockLightRegistry.get(it.block, SubtleLightType) != null }) {
            sectionLoadQueue.add(pos)
        } else {
            synchronized(scannedQueue) {
                scannedQueue.add(pos)
            }
        }
    }

    override fun deloadChunk(manager: LightManager, chunk: ChunkAccess) {
        super.deloadChunk(manager, chunk)
        synchronized(scannedRemoveQueue) {
            scannedRemoveQueue.add(chunk.pos)
        }
    }

    inner class Chunk(
        @JvmField
        val pos: SectionPos
    ) : HashMapBlockLightStorage<SubtleLightInfo, SubtleLight>(SubtleLightType), NativeResource {
        @JvmField
        var box: IRect3<Int>? = null
        @JvmField
        var dirty = true
        var x = 0

        override fun shouldCollectMeshGeometry(pos: SectionPos): Boolean {
            return pos == this.pos
        }

        override fun createLight(
            manager: LightManager,
            level: Level,
            state: BlockState,
            pos: IVec3<Int>,
            info: SubtleLightInfo
        ): SubtleLight? {
            if (info.enabled(state)) {
                val cullingMode = VibrancyConfig.subtleLightCullingMode

                if (
                    Direction.entries.all { dir ->
                        val pos = (pos + dir).toBlockPos()
                        cullingMode.test(level, pos, state, level.getBlockState(pos))
                    }
                ) {
                    return null
                }

                return SubtleLight(info, state, pos)
            } else {
                return null
            }
        }

        override fun loadChunk(manager: LightManager, chunk: ChunkAccess) {
            deloadChunk(manager, chunk)

            val section = chunk.getSection(chunk.getSectionIndexFromSectionY(pos.y))

            if (section.maybeHas { BlockLightRegistry.get(it.block, SubtleLightType) != null }) {
                val origin = pos.origin()

                for (x in 0 until 16) {
                    for (y in 0 until 16) {
                        for (z in 0 until 16) {
                            val state = section.getBlockState(x, y, z)

                            BlockLightRegistry.get(state.block, type)?.let { info ->
                                type.castInfo(info)?.let {
                                    addLight(
                                        manager,
                                        manager.getLevel()!!,
                                        state,
                                        IVec3(x + origin.x, y + origin.y, z + origin.z),
                                        it
                                    )
                                }
                            }
                        }
                    }
                }
            }

            dirty = true

            synchronized(scannedQueue) {
                scannedQueue.add(pos)
            }
        }

        override fun deloadChunk(manager: LightManager, chunk: ChunkAccess) {
            super.deloadChunk(manager, chunk)
            dirty = true
        }

        override fun addLight(
            manager: LightManager,
            level: Level,
            state: BlockState,
            pos: IVec3<Int>,
            info: SubtleLightInfo
        ) {
            super.addLight(manager, level, state, pos, info)
            dirty = true
        }

        override fun removeLight(manager: LightManager, level: Level, pos: IVec3<Int>): Boolean {
            if (super.removeLight(manager, level, pos)) {
                dirty = true
                return true
            } else {
                return false
            }
        }

        override fun reload(manager: LightManager, chunk: ChunkPos?) {
            if (chunk == null || (chunk.x == pos.x && chunk.z == pos.z)) {
                dirty = true
            }
        }

        override fun free() {
            tasks[pos]?.cancel()
        }

        override fun clear(manager: LightManager) {
            super.clear(manager)
            dirty = true
        }

        fun numActiveTasks() = 0 // TODO
    }
}