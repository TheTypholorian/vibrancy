package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferType
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.GlBuffer
import net.typho.big_shot_lib.api.client.opengl.shaders.GlShader
import net.typho.big_shot_lib.api.client.opengl.util.GlResourcePool
import net.typho.big_shot_lib.api.client.opengl.util.MeshUtil
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.ChunkedBlockLightStorage
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.shadows.BasicMesher
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.shadows.LightMesh
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class SubtleLightStorage : ChunkedBlockLightStorage<SubtleLightInfo, SubtleLightStorage.Chunk>(SubtleLightType) {
    @JvmField
    val dirty = HashSet<ChunkPos>()
    @JvmField
    val tasks = LinkedList<CompletableFuture<Consumer<RenderEventData>?>>()

    override fun createChunk(manager: LightManager, level: Level, pos: ChunkPos): Chunk {
        val chunk = Chunk(pos)

        val box = BlockBox(
            BlockPos(pos.minBlockX - 1, level.minBuildHeight, pos.minBlockZ - 1),
            BlockPos(pos.maxBlockX + 1, level.maxBuildHeight, pos.maxBlockZ + 1),
        )

        for (x in pos.x - 1..pos.x + 1) {
            for (z in pos.z - 1..pos.z + 1) {
                level.getChunk(x, z).findBlocks(BlockLightRegistry::has) { pos, state ->
                    val actualPos = BlockPos(pos)

                    if (box.contains(actualPos)) {
                        BlockLightRegistry.get(state.block, SubtleLightType)?.let { info ->
                            chunk.rawAddLight(manager, level, state, pos, info)
                        }
                    }
                }
            }
        }

        return chunk
    }

    override fun clear(manager: LightManager) {
        super.clear(manager)
        synchronized(dirty) {
            dirty.clear()
        }
    }

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        super.reload(manager, chunk)
        synchronized(dirty) {
            if (chunk == null) {
                dirty.addAll(chunks.keys)
            } else {
                dirty.add(chunk)
            }
        }
    }

    override fun loadChunk(manager: LightManager, chunk: LevelChunk) {
        super.loadChunk(manager, chunk)
        synchronized(dirty) {
            dirty.add(chunk.pos)
        }
    }

    override fun deloadChunk(manager: LightManager, chunk: LevelChunk) {
        super.deloadChunk(manager, chunk)
        synchronized(dirty) {
            dirty.add(chunk.pos)
        }
    }

    fun checkDirty(
        manager: LightManager,
        data: RenderEventData
    ) {
        tasks.removeIf { task ->
            if (task.isDone) {
                task.get()?.accept(data)
                return@removeIf true
            } else {
                return@removeIf false
            }
        }

        synchronized(dirty) {
            for (pos in dirty) {
                val chunk = getOrCreateChunk(manager, data.level, pos)

                fun impl(): Consumer<RenderEventData>? {
                    chunk.box = chunk.map.values.fold(null) { box, light ->
                        if (box == null) light.boundingBox else box.minmax(light.boundingBox)
                    }

                    if (chunk.map.isEmpty()) {
                        return null
                    }

                    val blocks = HashSet<BlockPos>()
                    val ssboBuffer = MemoryUtil.memAllocFloat(8 * chunk.size)

                    for (light in chunk.map.values) { // TODO fix concurrent mod except
                        if (light.shouldRender(chunk)) {
                            blocks.addAll(
                                light.shadowBox
                                    .map { BlockPos(it) }
                                    .filter {
                                        it.x >= pos.minBlockX && it.x <= pos.maxBlockX &&
                                                it.z >= pos.minBlockZ && it.z <= pos.maxBlockZ
                                    }
                            )

                            val color = light.color
                            val pos = light.absolutePos

                            ssboBuffer.put(color.x).put(color.y).put(color.z).put(0f)
                            ssboBuffer.put(pos.x).put(pos.y).put(pos.z).put(0f)
                        }
                    }

                    val faces = LinkedList<LightFace>()
                    BasicMesher(blocks).submit(manager, data.level, SubtleLight.SHADOW_PREDICATE, TextureUtil.INSTANCE.blockAtlas, {}, faces::add)
                    val task = chunk.mesh.value!!.build(data.level, faces)

                    return Consumer { data ->
                        task.run()
                        chunk.ssbo.upload(ssboBuffer.flip())
                        MemoryUtil.memFree(ssboBuffer)

                        val blitSettings = SubtleLightType.meshBlitSettings(data, chunk)
                        blitSettings.bind()
                        MeshUtil.SCREEN_MESH.draw()
                        blitSettings.unbind()
                        data.target.viewport() // TODO
                    }
                }

                if (Vibrancy.config.useMultithreading) {
                    tasks.add(CompletableFuture.supplyAsync(::impl))
                } else {
                    impl()?.accept(data)
                }
            }

            dirty.clear()
        }
    }

    inner class Chunk(
        @JvmField
        val pos: ChunkPos,
        @JvmField
        val mesh: GlResourcePool<LightMesh>.Handle = LightMesh.pool.poll(),
        @JvmField
        val ssbo: GlBuffer = GlBuffer(BufferType.SHADER_STORAGE_BUFFER, BufferUsage.STATIC_DRAW)
    ) : HashMapBlockLightStorage<SubtleLightInfo, SubtleLight>(SubtleLightType), NativeResource {
        @JvmField
        var box: AABB? = null

        fun render(data: RenderEventData, shader: GlShader): LightRenderResult {
            if (
                size > 0
                && box?.let { data.frustum.testAab(it.minPosition.toVector3f(), it.maxPosition.toVector3f()) } ?: true
            ) {
                mesh.value!!.draw(shader)

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
        ): SubtleLight? = if (info.enabled.apply(state)) SubtleLight(info, state, pos) else null

        private fun markDirty() {
            synchronized(dirty) {
                for (x in pos.x - 1..pos.x + 1) {
                    for (z in pos.z - 1..pos.z + 1) {
                        dirty.add(ChunkPos(x, z))
                    }
                }
            }
        }

        internal fun rawAddLight(
            manager: LightManager,
            level: Level,
            state: BlockState,
            pos: BlockPos,
            info: SubtleLightInfo
        ) {
            super.addLight(manager, level, state, pos, info)
        }

        override fun addLight(
            manager: LightManager,
            level: Level,
            state: BlockState,
            pos: BlockPos,
            info: SubtleLightInfo
        ) {
            super.addLight(manager, level, state, pos, info)
            markDirty()
        }

        override fun removeLight(manager: LightManager, level: Level, pos: BlockPos): Boolean {
            if (super.removeLight(manager, level, pos)) {
                markDirty()
                return true
            } else {
                return false
            }
        }

        override fun reload(manager: LightManager, chunk: ChunkPos?) {
        }

        override fun free() {
            mesh.release()
            ssbo.free()
        }
    }
}