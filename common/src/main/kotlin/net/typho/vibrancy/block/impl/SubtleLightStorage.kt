package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.ChunkedBlockLightStorage
import net.typho.vibrancy.block.HashMapBlockLightStorage
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource
import java.util.concurrent.CompletableFuture

class SubtleLightStorage : ChunkedBlockLightStorage<SubtleLightInfo, SubtleLightStorage.Chunk>(SubtleLightType) {
    @JvmField
    val dirty = HashSet<ChunkPos>()
    @JvmField
    val tasks = HashMap<ChunkPos, CompletableFuture<Runnable?>>()

    override fun createChunk(pos: ChunkPos) = Chunk(pos)

    override fun clear(manager: LightManager) {
        super.clear(manager)
        dirty.clear()
    }

    override fun reload(manager: LightManager) {
        super.reload(manager)
        dirty.addAll(chunks.keys)
    }

    override fun loadChunk(manager: LightManager, chunk: LevelChunk) {
        super.loadChunk(manager, chunk)
        dirty.add(chunk.pos)
    }

    override fun deloadChunk(manager: LightManager, chunk: LevelChunk) {
        super.deloadChunk(manager, chunk)
        dirty.add(chunk.pos)
    }

    fun checkDirty(
        manager: LightManager
    ) {
        tasks.values.removeIf { task ->
            if (task.isDone) {
                task.get()?.run()
                return@removeIf true
            } else {
                return@removeIf false
            }
        }

        val level = manager.getLevel() ?: return

        for (pos in dirty) {
            val newChunk = createChunk(pos)

            tasks.put(
                pos,
                CompletableFuture.supplyAsync {
                    level.getChunk(pos.x, pos.z)
                        .findBlocks({ BlockLightRegistry.has(it.block) }) { pos, state ->
                            val actualPos = BlockPos(pos)

                            BlockLightRegistry.get(state.block, SubtleLightType)?.let { info ->
                                newChunk.addLight(manager, state, actualPos, info)
                            }
                        }

                    if (newChunk.map.isEmpty()) {
                        return@supplyAsync null
                    }

                    val builder = newChunk.mesh.Builder()
                    val ssboBuffer = MemoryUtil.memAllocFloat(8 * newChunk.size)

                    for (light in newChunk.map.values) {
                        builder.cube(light.boundingBox)

                        val color = light.color
                        val pos = light.absolutePos

                        ssboBuffer.put(color.x).put(color.y).put(color.z).put(0f)
                        ssboBuffer.put(pos.x).put(pos.y).put(pos.z).put(0f)
                    }

                    return@supplyAsync Runnable {
                        builder.end()

                        newChunk.ssbo.upload(ssboBuffer.flip())

                        MemoryUtil.memFree(ssboBuffer)

                        chunks.put(pos, newChunk)?.free()
                    }
                }
            )?.cancel(true)
        }

        dirty.clear()
    }

    class Chunk(
        @JvmField
        val pos: ChunkPos,
        @JvmField
        val mesh: Mesh = Mesh(NeoVertexFormat.POSITION, GlShapeType.QUADS, BufferUsage.STATIC_DRAW),
        @JvmField
        val ssbo: GlBuffer = GlBuffer(BufferType.SHADER_STORAGE_BUFFER, BufferUsage.STATIC_DRAW)
    ) : HashMapBlockLightStorage<SubtleLightInfo, SubtleLight>(SubtleLightType), NativeResource {
        val box: AABB?
            get() = map.values.fold(null) { box, light -> if (box == null) light.boundingBox else box.minmax(light.boundingBox) }

        fun render(data: RenderEventData): LightRenderResult {
            if (
                size > 0
                && box?.let { data.frustum.testAab(it.minPosition.toVector3f(), it.maxPosition.toVector3f()) } ?: true
            ) {
                ssbo.bindBase(0)

                mesh.draw()

                return LightRenderResult(numRendered = size)
            } else {
                return LightRenderResult()
            }
        }

        override fun createLight(
            manager: LightManager,
            state: BlockState,
            pos: BlockPos,
            info: SubtleLightInfo
        ): SubtleLight {
            return SubtleLight(info, state, pos)
        }

        override fun reload(manager: LightManager) {
        }

        override fun free() {
            mesh.free()
            ssbo.free()
        }
    }
}