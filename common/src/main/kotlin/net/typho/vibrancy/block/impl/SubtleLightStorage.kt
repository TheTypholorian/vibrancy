package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.client.registration.events.RenderEventData
import net.typho.big_shot_lib.api.client.rendering.buffers.BufferType
import net.typho.big_shot_lib.api.client.rendering.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.rendering.buffers.GlBuffer
import net.typho.big_shot_lib.api.client.rendering.meshes.Mesh
import net.typho.big_shot_lib.api.client.rendering.meshes.NeoVertexFormat
import net.typho.big_shot_lib.api.client.rendering.util.GlShapeType
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.BlockLightStorage
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource
import java.util.concurrent.CompletableFuture

class SubtleLightStorage : BlockLightStorage<SubtleLightInfo> {
    @JvmField
    val meshes = HashMap<ChunkPos, ChunkMesh>()
    @JvmField
    val dirty = HashSet<ChunkPos>()
    @JvmField
    val tasks = HashMap<ChunkPos, CompletableFuture<Runnable>>()

    fun markDirty(pos: ChunkPos) {
        dirty.add(pos)
        dirty.add(ChunkPos(pos.x + 1, pos.z))
        dirty.add(ChunkPos(pos.x - 1, pos.z))
        dirty.add(ChunkPos(pos.x, pos.z + 1))
        dirty.add(ChunkPos(pos.x, pos.z - 1))
    }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: BlockPos,
        info: SubtleLightInfo
    ) {
        markDirty(ChunkPos(pos))
    }

    override fun removeLight(manager: LightManager, pos: BlockPos) {
        markDirty(ChunkPos(pos))
    }

    override fun rebuildShadows(manager: LightManager) {
    }

    override fun loadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        markDirty(chunk.pos)
    }

    override fun deloadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        markDirty(chunk.pos)
    }

    override fun clear(manager: LightManager) {
        meshes.values.forEach { chunk -> chunk.free() }
        meshes.clear()
        dirty.clear()
    }

    fun checkDirty(
        manager: LightManager
    ) {
        tasks.values.removeIf { task ->
            if (task.isDone) {
                task.get().run()
                return@removeIf true
            } else {
                return@removeIf false
            }
        }

        for (pos in dirty) {
            val mesh = meshes.computeIfAbsent(pos, ::ChunkMesh)

            tasks.put(
                pos, CompletableFuture.supplyAsync {
                    val chunk = manager.getLevel().getChunk(pos.x, pos.z)
                    val lights = HashMap<BlockPos, SubtleLight>()

                    chunk.findBlocks({ BlockLightRegistry.has(it.block) }) { pos, state ->
                        val actualPos = BlockPos(pos)

                        BlockLightRegistry.get(state.block)?.let { info ->
                            if (info is SubtleLightInfo) {
                                info.createBlockLight(manager, manager.getLevel(), state, actualPos)?.let { light ->
                                    lights[actualPos] = light
                                }
                            }
                        }
                    }

                    if (lights.isEmpty()) {
                        return@supplyAsync Runnable {
                            mesh.size = 0
                        }
                    }

                    val builder = mesh.mesh.Builder()
                    val ssboBuffer = MemoryUtil.memAllocFloat(8 * lights.size)

                    var box: AABB? = null

                    for (light in lights.values) {
                        box = if (box == null) {
                            light.getBoundingBox()
                        } else {
                            box.intersect(light.getBoundingBox())
                        }

                        builder.cube(light.getBoundingBox())

                        val color = light.color
                        val pos = light.getAbsolutePos()

                        ssboBuffer.put(color.x).put(color.y).put(color.z).put(0f)
                        ssboBuffer.put(pos.x).put(pos.y).put(pos.z).put(0f)
                    }

                    return@supplyAsync Runnable {
                        mesh.size = lights.size
                        mesh.box = box

                        builder.end()

                        mesh.ssbo.bind()
                        mesh.ssbo.upload(ssboBuffer.flip())
                        mesh.ssbo.unbind()

                        MemoryUtil.memFree(ssboBuffer)
                    }
            }
            )?.cancel(true)
        }

        dirty.clear()
    }

    override fun size(): Int {
        return meshes.values.sumOf { mesh -> mesh.size }
    }

    data class ChunkMesh(
        val pos: ChunkPos,
        val mesh: Mesh = Mesh(NeoVertexFormat.POSITION, GlShapeType.QUADS, BufferUsage.STATIC_DRAW),
        val ssbo: GlBuffer = GlBuffer(BufferType.SHADER_STORAGE_BUFFER, BufferUsage.STATIC_DRAW),
        var size: Int = 0,
        var box: AABB? = null
    ) : NativeResource {
        fun render(data: RenderEventData, manager: LightManager): LightRenderResult {
            if (
                size > 0
                && manager.inRenderDistance(pos, Vibrancy.config.blockLights.subtle.renderDistance.get())
                && box?.let { data.frustum.testAab(it.minPosition.toVector3f(), it.maxPosition.toVector3f()) || manager.inRenderDistance(pos, 6) } ?: true
            ) {
                ssbo.bindBase(0)

                mesh.bind()
                //mesh.ebo.bind()
                mesh.draw()
                //mesh.ebo.unbind()
                mesh.unbind()

                ssbo.unbindBase(0)

                return LightRenderResult(numRendered = size)
            } else {
                return LightRenderResult()
            }
        }

        override fun free() {
            mesh.free()
            ssbo.free()
        }
    }
}