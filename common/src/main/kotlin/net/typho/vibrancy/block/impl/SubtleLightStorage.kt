package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.*
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.BigShotLib.cube
import net.typho.big_shot_lib.api.impl.NeoIndexedBuffer
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.resource.BufferUsage
import net.typho.big_shot_lib.gl.resource.GlResourceType
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
        state: StateHolder<*, *>,
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

                    for (i in chunk.minSection until chunk.maxSection) {
                        val section = chunk.getSection(chunk.getSectionIndexFromSectionY(i))

                        if (section.maybeHas { BlockLightRegistry.has(it.block) }) {
                            val minPos = SectionPos.of(chunk.pos, i).origin()

                            for (x in 0 until LevelChunkSection.SECTION_WIDTH) {
                                for (y in 0 until LevelChunkSection.SECTION_HEIGHT) {
                                    for (z in 0 until LevelChunkSection.SECTION_WIDTH) {
                                        val state = section.getBlockState(x, y, z)

                                        BlockLightRegistry.get(state.block)?.let { info ->
                                            if (info is SubtleLightInfo) {
                                                val pos = BlockPos(
                                                    x + minPos.x,
                                                    y + minPos.y,
                                                    z + minPos.z
                                                )

                                                info.createBlockLight(manager, manager.getLevel(), state, pos)?.let { light ->
                                                    lights[pos] = light
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (lights.isEmpty()) {
                        return@supplyAsync Runnable {
                            mesh.size = 0
                        }
                    }

                    val buffer = ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE)
                    val builder = BufferBuilder(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
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

                        mesh.vbo.bind()
                        mesh.vbo.upload(builder.buildOrThrow())
                        VertexBuffer.unbind()

                        mesh.ssbo.bind()
                        mesh.ssbo.upload(MemoryUtil.memByteBuffer(ssboBuffer.flip()))
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
        val vbo: VertexBuffer = VertexBuffer(VertexBuffer.Usage.STATIC),
        val ssbo: NeoIndexedBuffer = NeoIndexedBuffer(null, GlResourceType.SHADER_STORAGE_BUFFER, BufferUsage.STATIC_DRAW),
        var size: Int = 0,
        var box: AABB? = null
    ) : NativeResource {
        fun render(manager: LightManager, stack: GlStack): LightRenderResult {
            if (
                size > 0
                && manager.inRenderDistance(pos, Vibrancy.config.blockLights.subtle.renderDistance.get())
                && box?.let { manager.inFrustum(it) || manager.inRenderDistance(pos, 6) } ?: true
            ) {
                ssbo.bindBase(stack, 0)

                vbo.bind()
                vbo.draw()

                return LightRenderResult(numRendered = size)
            } else {
                return LightRenderResult()
            }
        }

        override fun free() {
            vbo.close()
            ssbo.release()
        }
    }
}