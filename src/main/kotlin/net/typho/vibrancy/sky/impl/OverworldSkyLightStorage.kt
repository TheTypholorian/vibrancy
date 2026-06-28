package net.typho.vibrancy.sky.impl

//? if sable {
/*import dev.ryanhcode.sable.companion.SableCompanion
*///? }

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.Heightmap
import net.typho.big_shot_lib.api.math.IRect3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.collectors.SkyLightBlockMeshCollector
import net.typho.vibrancy.sky.ChunkedSkyLightStorage
import net.typho.vibrancy.sky.SkyLightStorage
import net.typho.vibrancy.util.BlockFace
import net.typho.vibrancy.util.GpuTask
import org.lwjgl.system.NativeResource

class OverworldSkyLightStorage : ChunkedSkyLightStorage<OverworldSkyLightInfo, OverworldSkyLightStorage.Chunk>(OverworldSkyLightType) {
    var info: OverworldSkyLightInfo? = null
        private set

    override fun createChunk(
        manager: LightManager,
        pos: ChunkPos
    ) = Chunk(pos, manager.getLevel()!!.minSectionY, manager.getLevel()!!.sectionsCount)

    override fun load(
        manager: LightManager,
        info: OverworldSkyLightInfo
    ) {
        this.info = info
    }

    override fun loadChunk(manager: LightManager, chunk: ChunkAccess) {
        for (x in (chunk.pos.x - 1)..(chunk.pos.x + 1)) {
            for (z in (chunk.pos.z - 1)..(chunk.pos.z + 1)) {
                getOrCreateChunk(manager, ChunkPos(x, z)).loadChunk(manager, manager.getLevel()!!.getChunk(x, z))
            }
        }
    }

    class Chunk(
        @JvmField
        val pos: ChunkPos,
        minSection: Int,
        height: Int
    ) : SkyLightStorage<OverworldSkyLightInfo>, NativeResource {
        inner class Section(
            minSection: Int,
            y: Int
        ) : NativeResource {
            @JvmField
            val pos = SectionPos.of(this@Chunk.pos.x, y + minSection, this@Chunk.pos.z)

            override fun free() {
            }
        }

        @JvmField
        val sections = Array(height) { Section(minSection, it) }
        @JvmField
        var dirty = true // TODO
        var box: IRect3<Int>? = null
            private set
        var blockEntities: MutableSet<BlockPos> = hashSetOf()
            private set
        private var asyncTask: GpuTask<IRect3<Int>?>? = null

        override fun free() {
            asyncTask?.cancel()
            sections.forEach { it.free() }
        }

        fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

        fun checkIfFinished(): Boolean {
            asyncTask?.let { task ->
                if (task.isDoneOrCancelled()) {
                    try {
                        box = task.finish()
                    } catch (e: NullPointerException) {
                        Vibrancy.LOGGER.warn("Error finishing sky light task at $pos", e)
                    }

                    asyncTask = null
                    return true
                }
            }

            return false
        }

        private fun rebuildBlocksAsyncImpl(
            isCancelled: () -> Boolean,
            manager: LightManager
        ): Pair<AutoCloseable, () -> IRect3<Int>?> {
            val level = manager.getLevel() ?: throw NullPointerException("No level?")
            var box: IRect3<Int>? = null

            val lightX = pos.minBlockX - 1
            val lightZ = pos.minBlockZ - 1
            val lightIterPos = BlockPos.MutableBlockPos()
            val lightArray = Array(18) { x -> Array(18) { z ->
                val y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x + lightX, z + lightZ)
                lightIterPos.set(x + lightX, level.minY, z + lightZ)

                while (lightIterPos.y <= y) {
                    if (level.getBrightness(LightLayer.SKY, lightIterPos) > 0) {
                        return@Array lightIterPos.y
                    }

                    lightIterPos.move(Direction.UP)
                }

                return@Array y
            } }

            fun couldHaveLight(x: Int, y: Int, z: Int): Boolean {
                return lightArray[x - lightX][z - lightZ] <= y
            }

            val lightFaces = Array(sections.size) { arrayListOf<BlockFace>() }
            val translucentFaces = Array(sections.size) { arrayListOf<BlockFace>() }
            val mesher = SkyLightBlockMeshCollector(pos) // TODO
            /*
            if (!mesher.submit(
                    isCancelled,
                    manager,
                    level,
                    NeoAtlas.blocks,
                    object : BlockMeshCollector.Consumer {
                        override val predicate: BlockMeshCollector.Predicate = object : BlockMeshCollector.Predicate {
                            override fun shouldCastBlock(
                                level: Level,
                                pos: BlockPos,
                                state: BlockState?
                            ): Boolean {
                                if (
                                    couldHaveLight(pos.x, pos.y + 1, pos.z) ||
                                    couldHaveLight(pos.x - 1, pos.y, pos.z) ||
                                    couldHaveLight(pos.x + 1, pos.y, pos.z) ||
                                    couldHaveLight(pos.x, pos.y, pos.z - 1) ||
                                    couldHaveLight(pos.x, pos.y, pos.z + 1) ||
                                    couldHaveLight(pos.x, pos.y, pos.z)
                                ) {
                                    val pos1 = IVec3(pos)
                                    box = box?.include(pos1) ?: IRect3(pos1, pos1)
                                    return true
                                }

                                return false
                            }
                        }

                        override fun collect(
                            faces: Iterable<LightFace>,
                            section: SectionPos,
                            block: BlockPos,
                            translucent: Boolean
                        ) {
                            // TODO
                            if (translucent) {
                                for (face in faces) {
                                    if (face.any { (it.light ushr 16) and 0xFFFF != 0 }) {
                                        translucentFaces[SectionPos.blockToSectionCoord(block.y) - level.minSection].add(face)
                                    }
                                }
                            } else {
                                for (face in faces) {
                                    lightFaces[SectionPos.blockToSectionCoord(block.y) - level.minSection].add(face)
                                }
                            }
                        }
                    }
                )) {
                return AutoCloseable { } to { null }
            }
             */
            blockEntities = mesher.blockEntities

            return AutoCloseable {
            } to {
                box
            }
        }

        override fun load(
            manager: LightManager,
            info: OverworldSkyLightInfo
        ) {
            dirty = true
        }

        override fun reload(manager: LightManager) {
            dirty = true
        }

        override fun loadChunk(
            manager: LightManager,
            chunk: ChunkAccess
        ) {
            dirty = true
        }

        override fun deloadChunk(
            manager: LightManager,
            chunk: ChunkAccess
        ) {
            free()
        }

        override fun clear(manager: LightManager) {
            free()
        }
    }
}