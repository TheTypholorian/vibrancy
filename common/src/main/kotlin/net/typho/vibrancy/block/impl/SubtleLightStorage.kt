package net.typho.vibrancy.block.impl

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.iterator
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.plus
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.ChunkedBlockLightStorage
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.shadows.BasicMesher
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.shadows.LightMesh
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
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
        return Chunk(pos).also { it.scan(level, manager) }
    }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: AbstractVec3<Int>,
        info: SubtleLightInfo
    ) {
        val chunks = hashSetOf(
            ChunkPos(pos.blockPos),
            ChunkPos((pos + NeoDirection.NORTH).blockPos),
            ChunkPos((pos + NeoDirection.SOUTH).blockPos),
            ChunkPos((pos + NeoDirection.EAST).blockPos),
            ChunkPos((pos + NeoDirection.WEST).blockPos),
            ChunkPos((pos + NeoDirection.NORTH + NeoDirection.EAST).blockPos),
            ChunkPos((pos + NeoDirection.SOUTH + NeoDirection.WEST).blockPos),
            ChunkPos((pos + NeoDirection.EAST + NeoDirection.SOUTH).blockPos),
            ChunkPos((pos + NeoDirection.WEST + NeoDirection.NORTH).blockPos)
        )

        chunks.forEach { getOrCreateChunk(manager, level, it).addLight(manager, level, state, pos, info) }
    }

    override fun removeLight(
        manager: LightManager,
        level: Level,
        pos: AbstractVec3<Int>,
    ): Boolean {
        val chunks = hashSetOf(
            ChunkPos(pos.blockPos),
            ChunkPos((pos + NeoDirection.NORTH).blockPos),
            ChunkPos((pos + NeoDirection.SOUTH).blockPos),
            ChunkPos((pos + NeoDirection.EAST).blockPos),
            ChunkPos((pos + NeoDirection.WEST).blockPos),
            ChunkPos((pos + NeoDirection.NORTH + NeoDirection.EAST).blockPos),
            ChunkPos((pos + NeoDirection.SOUTH + NeoDirection.WEST).blockPos),
            ChunkPos((pos + NeoDirection.EAST + NeoDirection.SOUTH).blockPos),
            ChunkPos((pos + NeoDirection.WEST + NeoDirection.NORTH).blockPos)
        )

        return chunks.fold(false) { accum, chunkPos -> accum or getOrCreateChunk(manager, level, chunkPos).removeLight(manager, level, pos) }
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
                        box?.include(light.boundingBox) ?: light.boundingBox
                    }

                    if (chunk.map.isEmpty()) {
                        return null
                    }

                    val blocks = HashSet<AbstractVec3<Int>>()

                    chunk.map.values.forEach { light ->
                        light.shadowBox.iterator().forEach { block ->
                            if (
                                block.x >= pos.minBlockX && block.x <= pos.maxBlockX &&
                                block.z >= pos.minBlockZ && block.z <= pos.maxBlockZ
                            ) {
                                blocks.add(block)
                            }
                        }
                    }

                    val faces = LinkedList<LightFace>()
                    BasicMesher(blocks).submit(
                        manager,
                        data.level,
                        NeoAtlas.blocks,
                        SubtleLightFacePredicate,
                        faces::add
                    )
                    val task = chunk.mesh.build(faces)
                    val buffer = NeoBuffer.Native(chunk.size.toLong() * 8 * Float.SIZE_BYTES)

                    buffer.write().run {
                        for (light in chunk.map.values) {
                            val color = light.color
                            val pos = light.absolutePos

                            writeFloat(pos.x)
                            writeFloat(pos.y)
                            writeFloat(pos.z)
                            writeFloat(0f)

                            writeFloat(color.x)
                            writeFloat(color.y)
                            writeFloat(color.z)
                            writeFloat(0f)
                        }
                    }

                    return Consumer { data ->
                        val atlasResult = task()

                        chunk.ssbo.bind(GlBufferTarget.SHADER_STORAGE_BUFFER).use { ssbo ->
                            ssbo.bufferData(buffer, GlBufferUsage.STATIC_DRAW)
                            buffer.free()
                        }

                        if (faces.isNotEmpty()) {
                            NeoGlFramebuffer().use { fbo ->
                                fbo.bind(NeoRect2i(0, 0, chunk.mesh.texture.width, chunk.mesh.texture.height)).use { fbo ->
                                    fbo.colorAttachments[0] = chunk.mesh.texture
                                    fbo.checkStatus().throwIfError()

                                    SubtleLightType.meshBlitDrawState.bind().use {
                                        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunk.ssbo.glId)

                                        Mesh(
                                            LightMesh.BLIT_VERTEX_FORMAT,
                                            GlBeginMode.QUADS,
                                            GlBufferWriter.Mode.REGULAR,
                                            GlBufferUsage.STREAM_DRAW
                                        ).use { mesh ->
                                            LightMesh.initBlitMesh(mesh, LightMesh.LightBlitInfo(atlasResult, faces))
                                            mesh.draw()
                                        }
                                    }
                                }
                            }
                        }
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
        val pos: ChunkPos
    ) : HashMapBlockLightStorage<SubtleLightInfo, SubtleLight>(SubtleLightType), NativeResource {
        @JvmField
        var box: AbstractRect3<Int>? = null
        @JvmField
        val mesh = LightMesh()
        @JvmField
        val ssbo = NeoGlBuffer()

        fun scan(level: Level, manager: LightManager) {
            map.clear()

            val box = NeoRect3i(
                pos.minBlockX - 1, level.minBuildHeight, pos.minBlockZ - 1,
                pos.maxBlockX + 1, level.maxBuildHeight, pos.maxBlockZ + 1
            )

            for (x in pos.x - 1..pos.x + 1) {
                for (z in pos.z - 1..pos.z + 1) {
                    level.getChunk(x, z).findBlocks(BlockLightRegistry::has) { pos, state ->
                        val pos = NeoVec3i(pos)

                        if (box.contains(pos)) {
                            BlockLightRegistry.get(state.block, SubtleLightType)?.let { info ->
                                rawAddLight(manager, level, state, pos, info)
                            }
                        }
                    }
                }
            }
        }

        fun render(data: RenderEventData, shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit) {
            if (
                size > 0
                // TODO
                //&& box?.let { data.frustum.testAab(it.min.toFloat().toJOML(), it.max.toFloat().toJOML()) } ?: true
            ) {
                mesh.draw(shader)
                debugOut("lightsRendered", size)
                debugOut("chunksRendered", 1)
            }
        }

        override fun createLight(
            manager: LightManager,
            state: BlockState,
            pos: AbstractVec3<Int>,
            info: SubtleLightInfo
        ): SubtleLight? = if (info.enabled.apply(state)) SubtleLight(info, state, pos) else null

        override fun loadChunk(manager: LightManager, chunk: LevelChunk) {
            scan(chunk.level!!, manager)
        }

        internal fun rawAddLight(
            manager: LightManager,
            level: Level,
            state: BlockState,
            pos: AbstractVec3<Int>,
            info: SubtleLightInfo
        ) {
            super.addLight(manager, level, state, pos, info)
        }

        override fun addLight(
            manager: LightManager,
            level: Level,
            state: BlockState,
            pos: AbstractVec3<Int>,
            info: SubtleLightInfo
        ) {
            super.addLight(manager, level, state, pos, info)
            dirty.add(this.pos)
        }

        override fun removeLight(manager: LightManager, level: Level, pos: AbstractVec3<Int>): Boolean {
            if (super.removeLight(manager, level, pos)) {
                dirty.add(this.pos)
                return true
            } else {
                return false
            }
        }

        override fun reload(manager: LightManager, chunk: ChunkPos?) {
        }

        override fun free() {
            mesh.free()
            ssbo.free()
        }
    }
}