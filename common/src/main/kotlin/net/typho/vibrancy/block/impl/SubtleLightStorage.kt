package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
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
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.BlockLightStorage
import net.typho.vibrancy.block.BlockRenderResult
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource

class SubtleLightStorage : BlockLightStorage<SubtleLightInfo> {
    @JvmField
    val meshes = HashMap<ChunkPos, ChunkMesh>()
    @JvmField
    val dirty = HashSet<ChunkPos>()

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
        for (pos in dirty) {
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

            val mesh = meshes.computeIfAbsent(pos, ::ChunkMesh)

            if (lights.isEmpty()) {
                mesh.size = 0
                continue
            }

            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
            val buffer = MemoryUtil.memAllocFloat(8 * lights.size)

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

                buffer.put(color.x).put(color.y).put(color.z).put(0f)
                buffer.put(pos.x).put(pos.y).put(pos.z).put(0f)
            }

            mesh.size = lights.size
            mesh.box = box

            mesh.vbo.bind()
            mesh.vbo.upload(builder.buildOrThrow())
            VertexBuffer.unbind()

            mesh.ssbo.bind()
            mesh.ssbo.upload(MemoryUtil.memByteBuffer(buffer.flip()))
            mesh.ssbo.unbind()

            MemoryUtil.memFree(buffer)
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
        fun render(manager: LightManager, stack: GlStack): BlockRenderResult {
            if (size > 0 && manager.inRenderDistance(pos, Vibrancy.config.blockLights.subtle.renderDistance.get())) {
                ssbo.bindBase(stack, 0)

                vbo.bind()
                vbo.draw()

                return BlockRenderResult(numRendered = size)
            } else {
                return BlockRenderResult()
            }
        }

        override fun free() {
            vbo.close()
            ssbo.release()
        }
    }
}