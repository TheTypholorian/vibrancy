package net.typho.vibrancy.block.impl

import dev.ryanhcode.sable.companion.SableCompanion
import net.minecraft.client.Minecraft
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.iterator
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.*
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.ChunkedBlockLightStorage
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.collectors.IterationBlockMeshCollector
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.LightTexture
import net.typho.vibrancy.util.VibrancyThreadPool
import org.joml.Matrix4f
import org.joml.Quaternionf
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

    override fun createChunk(manager: LightManager, pos: ChunkPos): Chunk {
        return Chunk(pos)
    }

    internal fun markBlockDirty(chunk: ChunkPos, pos: IVec3<Int>) {
        dirty.add(chunk)

        val relative = NeoVec2i(pos.x and 15, pos.z and 15)

        if (relative.x == 0) {
            if (relative.y == 0) {
                dirty.add(ChunkPos(chunk.x - 1, chunk.z - 1))
            }

            if (relative.y == 15) {
                dirty.add(ChunkPos(chunk.x - 1, chunk.z + 1))
            }

            dirty.add(ChunkPos(chunk.x - 1, chunk.z))
        } else if (relative.x == 15) {
            if (relative.y == 0) {
                dirty.add(ChunkPos(chunk.x + 1, chunk.z - 1))
            }

            if (relative.y == 15) {
                dirty.add(ChunkPos(chunk.x + 1, chunk.z + 1))
            }

            dirty.add(ChunkPos(chunk.x + 1, chunk.z))
        }

        if (relative.y == 0) {
            dirty.add(ChunkPos(chunk.x, chunk.z - 1))
        } else if (relative.y == 15) {
            dirty.add(ChunkPos(chunk.x, chunk.z + 1))
        }
    }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
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

        chunks.forEach { getOrCreateChunk(manager, it).addLight(manager, level, state, pos, info) }
    }

    override fun removeLight(
        manager: LightManager,
        level: Level,
        pos: IVec3<Int>,
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

        return chunks.fold(false) { accum, chunkPos -> accum or getOrCreateChunk(manager, chunkPos).removeLight(manager, level, pos) }
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

    override fun loadChunk(manager: LightManager, chunk: ChunkAccess) {
        super.loadChunk(manager, chunk)
        synchronized(dirty) {
            dirty.add(chunk.pos)
        }
    }

    override fun deloadChunk(manager: LightManager, chunk: ChunkAccess) {
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
                val chunk = getOrCreateChunk(manager, pos)

                fun impl(): Consumer<RenderEventData>? {
                    synchronized(chunk.map) {
                        chunk.box = chunk.map.values.fold(null) { box, light ->
                            box?.include(light.boundingBox) ?: light.boundingBox
                        }

                        if (chunk.map.isEmpty()) {
                            return null
                        }

                        val blocks = HashSet<IVec3<Int>>()

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

                        val lightFaces = arrayListOf<LightFace>()
                        val origin = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)
                        IterationBlockMeshCollector(origin, blocks).submit(
                            manager,
                            data.level!!,
                            NeoAtlas.blocks,
                            object : BlockMeshCollector.Consumer {
                                override val predicate: BlockMeshCollector.Predicate = SubtleLightMeshCollectorPredicate

                                override fun collect(faces: Iterable<LightFace>) {
                                    lightFaces.addAll(faces)
                                }
                            }
                        )
                        val task = chunk.mesh.lazyUpload(lightFaces)
                        val buffer = NeoBuffer.GCNative(chunk.size.toLong() * 8 * Float.SIZE_BYTES)

                        buffer.write().run {
                            for (light in chunk.map.values) {
                                val color = light.color
                                val pos = (light.pos - origin).toFloat() + light.offset

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

                            if (lightFaces.isNotEmpty()) {
                                chunk.lightTexture.resize(atlasResult.size.x, atlasResult.size.y)
                                chunk.lightTexture.framebuffer.bind(NeoRect2i(0, 0, chunk.lightTexture.width, chunk.lightTexture.height)).use { fbo ->
                                    SubtleLightType.meshBlitDrawState.bind().use {
                                        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, chunk.ssbo.glId)

                                        Mesh(
                                            LightMesh.BLIT_VERTEX_FORMAT,
                                            GlBeginMode.QUADS,
                                            GlBufferWriter.Mode.REGULAR,
                                            GlBufferUsage.STREAM_DRAW
                                        ).use { mesh ->
                                            LightMesh.initBlitMesh(mesh, LightMesh.MeshData(lightFaces, atlasResult))
                                            mesh.draw()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (VibrancyConfig.useMultithreading) {
                    tasks.add(CompletableFuture.supplyAsync(::impl, VibrancyThreadPool))
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
        val mesh = LightMesh(GlBufferUsage.STATIC_DRAW)
        @JvmField
        val lightTexture = LightTexture()
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
                                addLight(manager, level, state, pos, info)
                            }
                        }
                    }
                }
            }
        }

        fun render(data: RenderEventData, shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit) {
            if (
                size > 0
                //&& box?.let { data.frustum.testAab((it.min.toFloat() - data.camera.pos).toJOML(), (it.max.toFloat() - data.camera.pos).toJOML()) } ?: true
            ) {
                val blockPos = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)
                val subLevel = SableCompanion.INSTANCE.getContainingClient(pos)

                if (subLevel == null) {
                    shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((blockPos.toFloat() - data.camera.pos).toJOML(), Matrix4f())) }
                } else {
                    val tickDelta = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(false)
                    val pose = subLevel.renderPose(tickDelta)
                    val orientation = Quaternionf(pose.orientation())
                    val pos = NeoVec3d(pose.transformPosition(blockPos.toDouble().toJOML()))
                    shader.setUniform("ModelViewMat") {
                        set(
                            data.modelViewMat
                                .translate((pos - data.camera.pos.toDouble()).toFloat().toJOML(), Matrix4f())
                                .rotate(orientation)
                        )
                    }
                }

                shader.setTexture(1, GlTextureBinding.FromInstance(lightTexture, GlTextureTarget.TEXTURE_2D))
                mesh.draw()
                debugOut("lightsRendered", size)
                debugOut("chunksRendered", 1)
            }
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
                    NeoDirection.entries.all { dir ->
                        val pos = pos + dir
                        cullingMode.test(level, pos, state, level.getBlockState(pos.blockPos))
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
            scan(manager.getLevel()!!, manager)
        }

        override fun addLight(
            manager: LightManager,
            level: Level,
            state: BlockState,
            pos: IVec3<Int>,
            info: SubtleLightInfo
        ) {
            super.addLight(manager, level, state, pos, info)
            markBlockDirty(this.pos, pos)
        }

        override fun removeLight(manager: LightManager, level: Level, pos: IVec3<Int>): Boolean {
            if (super.removeLight(manager, level, pos)) {
                markBlockDirty(this.pos, pos)
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