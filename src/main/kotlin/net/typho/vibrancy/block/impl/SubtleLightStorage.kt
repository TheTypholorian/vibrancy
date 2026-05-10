package net.typho.vibrancy.block.impl

//? if 1.21 {
import dev.ryanhcode.sable.companion.SableCompanion
import net.typho.big_shot_lib.api.math.vec.NeoVec3d
import net.typho.vibrancy.Vibrancy
import org.joml.Quaternionf
//? }

import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.core.BlockPos
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlDataType
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.iterator
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.ChunkedBlockLightStorage
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.util.GlTask
import net.typho.vibrancy.util.VibrancyThreadPool
import org.joml.Matrix4f
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.system.NativeResource
import java.util.concurrent.CompletableFuture
import kotlin.use

class SubtleLightStorage : ChunkedBlockLightStorage<SubtleLightInfo, SubtleLightStorage.Chunk>(SubtleLightType) {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("LightIndex", NeoVertexFormat.Element.create(0, GlDataType.UNSIGNED_INT, null, 1))
            .add("Color", NeoVertexFormat.Element.COLOR)
            .add("Normal", NeoVertexFormat.Element.NORMAL)
            .build()
    }

    override fun createChunk(manager: LightManager, pos: ChunkPos): Chunk {
        return Chunk(pos)
    }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: SubtleLightInfo
    ) {
        getOrCreateChunk(manager, ChunkPos(pos.blockPos)).addLight(manager, level, state, pos, info)
    }

    override fun removeLight(
        manager: LightManager,
        level: Level,
        pos: IVec3<Int>,
    ): Boolean {
        return getOrCreateChunk(manager, ChunkPos(pos.blockPos)).removeLight(manager, level, pos)
    }

    fun checkDirty(
        manager: LightManager,
        data: RenderEventData,
        profiler: ProfilerFiller
    ) {
        profiler.push("finish")
        for ((pos, chunk) in chunks) {
            chunk.task?.let {
                if (it.isDoneOrCancelled()) {
                    it.finish()
                    chunk.task = null
                }
            }
        }
        profiler.pop()

        profiler.push("submit")
        checkDirty@ for ((pos, chunk) in chunks) {
            if (chunk.dirty) {
                for (x in (pos.x - 1)..(pos.x + 1)) {
                    for (z in (pos.z - 1)..(pos.z + 1)) {
                        if (!data.level!!.hasChunk(x, z)) {
                            continue@checkDirty
                        }
                    }
                }

                chunk.dirty = false

                fun impl(isCancelled: () -> Boolean, profiler: ProfilerFiller?): Pair<AutoCloseable, () -> Unit> {
                    synchronized(chunk.map) {
                        profiler?.push("fold")
                        chunk.box = chunk.map.values.fold(null) { box, light ->
                            box?.include(light.boundingBox) ?: light.boundingBox
                        }
                        profiler?.pop()

                        if (isCancelled() || chunk.map.isEmpty()) {
                            return AutoCloseable { } to { }
                        }

                        val quads = arrayListOf<Pair<NeoBakedQuad, Int>>()
                        val origin = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)

                        val lights = chunk.map.values.toList()

                        profiler?.push("collect")

                        val modelCache = hashMapOf<BlockPos, List<NeoBakedQuad>>()

                        lights.forEachIndexed { index, light ->
                            if (isCancelled()) {
                                return AutoCloseable { } to { }
                            }

                            light.shadowBox.iterator().forEach { block ->
                                //if (
                                //    block.x >= pos.minBlockX && block.x <= pos.maxBlockX &&
                                //    block.z >= pos.minBlockZ && block.z <= pos.maxBlockZ
                                //) {
                                val model = modelCache.computeIfAbsent(block.blockPos) {
                                    val quads = arrayListOf<NeoBakedQuad>()
                                    val block1 = BlockPos.MutableBlockPos().set(block.blockPos)
                                    BlockMeshCollector.collectLightFaces(
                                        manager,
                                        data.level!!.getBlockState(block1),
                                        data.level!!,
                                        block1,
                                        block - origin,
                                        NeoAtlas.blocks,
                                        true,
                                        object : BlockMeshCollector.Consumer {
                                            override val predicate = object : BlockMeshCollector.Predicate {
                                                override fun shouldCastBlock(
                                                    level: Level,
                                                    pos: BlockPos.MutableBlockPos,
                                                    state: BlockState?
                                                ): Boolean {
                                                    return true
                                                }

                                                override fun shouldCastFace(
                                                    face: NeoDirection?,
                                                    level: Level,
                                                    pos: BlockPos.MutableBlockPos,
                                                    state: BlockState?
                                                ): Boolean {
                                                    return face == null || BlockUtil.INSTANCE.shouldRenderFace(
                                                        level,
                                                        pos,
                                                        face,
                                                        state ?: level.getBlockState(pos)
                                                    )
                                                }
                                            }

                                            override fun collect(
                                                faces: Iterable<LightFace>,
                                                origin: BlockMeshCollector.FaceOrigin
                                            ) {
                                                faces.mapTo(quads) { it.quad }
                                            }
                                        }
                                    )
                                    quads
                                }
                                model.mapTo(quads) { it to index }
                                //}
                            }
                        }
                        profiler?.pop()

                        if (isCancelled()) {
                            return AutoCloseable { } to { }
                        }

                        profiler?.push("ssbo")
                        val buffer = NeoBuffer.GCNative(chunk.size.toLong() * 8 * Float.SIZE_BYTES)

                        buffer.write().run {
                            for (light in lights) {
                                if (isCancelled()) {
                                    return buffer to { }
                                }

                                val color = light.color
                                val pos = (light.pos - origin).toFloat() + light.offset

                                writeFloat(pos.x)
                                writeFloat(pos.y)
                                writeFloat(pos.z)
                                writeInt(light.shape)

                                writeFloat(color.x)
                                writeFloat(color.y)
                                writeFloat(color.z)
                                writeFloat(0f)
                            }
                        }
                        profiler?.pop()

                        if (isCancelled()) {
                            return buffer to { }
                        }

                        profiler?.push("upload")
                        val task = chunk.lazyUpload(isCancelled, quads)
                        profiler?.pop()

                        return AutoCloseable {
                            buffer.free()
                            task.first.close()
                        } to {
                            task.second()

                            chunk.ssbo.bind(GlBufferTarget.SHADER_STORAGE_BUFFER).use { ssbo ->
                                ssbo.bufferData(buffer, GlBufferUsage.STATIC_DRAW)
                            }
                        }
                    }
                }

                if (VibrancyConfig.useMultithreading) {
                    chunk.task?.cancel()
                    chunk.task = VibrancyThreadPool.submit(data, pos, manager) { impl(it, null) }
                } else {
                    val result = impl({ false }, profiler)
                    result.second()
                    result.first.close()
                }
            }
        }
        profiler.pop()
    }

    class Chunk(
        @JvmField
        val pos: ChunkPos
    ) : HashMapBlockLightStorage<SubtleLightInfo, SubtleLight>(SubtleLightType), NativeResource {
        @JvmField
        var box: AbstractRect3<Int>? = null
        @JvmField
        val mesh = Mesh(
            VERTEX_FORMAT,
            GlBeginMode.QUADS,
            GlBufferWriter.Mode.REGULAR,
            GlBufferUsage.STATIC_DRAW
        )
        @JvmField
        val ssbo = NeoGlBuffer()
        var dirty = true
            internal set
        var task: GlTask<Unit>? = null
            internal set

        fun lazyUpload(isCancelled: () -> Boolean, quads: Collection<Pair<NeoBakedQuad, Int>>): Pair<AutoCloseable, () -> Unit> {
            val vertexBuffer = NeoBuffer.GCNative(quads.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            vertexBuffer.write().run {
                quads.forEachIndexed { index, quad ->
                    if (isCancelled()) {
                        return vertexBuffer to { }
                    }

                    for (vertex in quad.first.vertices) {
                        writeFloat(vertex.pos.x)
                        writeFloat(vertex.pos.y)
                        writeFloat(vertex.pos.z)
                        writeFloat(vertex.textureUV!!.x)
                        writeFloat(vertex.textureUV!!.y)
                        writeInt(quad.second)
                        writeInt(vertex.color!!.toRGBA())
                        writeByte((vertex.normal!!.x * 127).toInt())
                        writeByte((vertex.normal!!.y * 127).toInt())
                        writeByte((vertex.normal!!.z * 127).toInt())
                    }
                }
            }

            val indices = mesh.generateIndices(quads.size * 4)

            return AutoCloseable {
                vertexBuffer.free()
                indices.first.free()
            } to {
                mesh.rawUpload(quads.size * 6, indices.second, vertexBuffer, indices.first)
            }
        }

        fun render(manager: LightManager, data: RenderEventData, shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit, profiler: ProfilerFiller) {
            if (
                size > 0
                && box?.let { manager.testFrustum(pos, data, it) } ?: true
            ) {
                debugOut("lightsRendered", size)
                debugOut("chunksRendered", 1)

                profiler.push("transforms")
                val blockPos = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)

                //? if 1.21 {
                val subLevel = SableCompanion.INSTANCE.getContainingClient(pos)

                if (subLevel == null) {
                    shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((blockPos.toFloat() - data.camera.pos).toJOML(), Matrix4f())) }
                    shader.setUniform("CameraPos") { setFloatVec(data.camera.pos - blockPos.toFloat()) }
                } else {
                    val pose = subLevel.renderPose(Vibrancy.tickDelta)
                    val orientation = Quaternionf(pose.orientation())
                    val pos = NeoVec3d(pose.transformPosition(blockPos.toDouble().toJOML()))
                    shader.setUniform("ModelViewMat") {
                        set(
                            data.modelViewMat
                                .translate((pos - data.camera.pos.toDouble()).toFloat().toJOML(), Matrix4f())
                                .rotate(orientation)
                        )
                    }
                    shader.setUniform("CameraPos") { setFloatVec(data.camera.pos - pos.toFloat()) }
                }
                //? } else {
                /*shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((blockPos.toFloat() - data.camera.pos).toJOML(), Matrix4f())) }
                shader.setUniform("CameraPos") { setFloatVec(data.camera.pos - blockPos.toFloat()) }
                *///? }
                profiler.pop()

                profiler.push("uniforms")
                //shader.setTexture(1, GlTextureBinding.FromInstance(lightTexture, GlTextureTarget.TEXTURE_2D))
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssbo.glId)
                shader.setTexture(0, GlTextureBinding.FromInstance(
                    NeoAtlas.blocks,
                    GlTextureTarget.TEXTURE_2D
                ))
                profiler.pop()

                profiler.push("draw")
                mesh.draw()
                profiler.pop()
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
                        val pos = (pos + dir).blockPos
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
            super.loadChunk(manager, chunk)
            dirty = true
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
            dirty = true
        }

        override fun free() {
            mesh.free()
        }

        override fun clear(manager: LightManager) {
            super.clear(manager)
            dirty = true
        }
    }
}