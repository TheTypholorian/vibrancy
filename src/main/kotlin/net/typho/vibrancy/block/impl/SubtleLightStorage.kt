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
import net.minecraft.core.SectionPos
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
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.block.SectionedBlockLightStorage
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.util.ChunkSectionCache
import net.typho.vibrancy.util.GlTask
import net.typho.vibrancy.util.SectionMeshCache
import net.typho.vibrancy.util.VibrancyThreadPool
import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.system.NativeResource
import kotlin.use

class SubtleLightStorage : SectionedBlockLightStorage<SubtleLightInfo, SubtleLightStorage.Chunk>(SubtleLightType) {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION) // 12 bytes
            .add("UV0", LightMesh.COMPACT_TEXTURE_UV) // 4 bytes
            .add("LightIndex", LightMesh.LIGHT_INDEX) // 2 bytes
            .add("Color", NeoVertexFormat.Element.COLOR) // 4 bytes
            .add("Normal", NeoVertexFormat.Element.NORMAL) // 3 bytes
            .build()
    }

    @JvmField
    val tasks = hashMapOf<SectionPos, GlTask<Unit>>()
    @JvmField
    val scannedQueue = hashSetOf<SectionPos>()
    @JvmField
    val scannedRemoveQueue = hashSetOf<ChunkPos>()
    @JvmField
    val scanned = hashSetOf<SectionPos>()
    @JvmField
    val sectionLoadQueue = hashSetOf<SectionPos>()
    @JvmField
    val sectionLoadTasks = hashSetOf<GlTask<Unit>>()

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

    fun checkDirty(
        manager: LightManager,
        data: RenderEventData,
        profiler: ProfilerFiller
    ) {
        profiler.push("applyScannedSet")
        synchronized(scannedQueue) {
            scanned.addAll(scannedQueue)
            scannedQueue.clear()
        }
        synchronized(scannedRemoveQueue) {
            for (pos in scannedRemoveQueue) {
                scanned.removeIf { it.x == pos.x && it.z == pos.z }
            }
            scannedRemoveQueue.clear()
        }
        profiler.pop()

        profiler.push("loadBlocks")
        val distance = manager.getGridRenderDistance(VibrancyConfig.subtleLightsRenderDistance).toFloat()

        sectionLoadQueue.removeIf { pos ->
            if (manager.inGridRenderDistance(data, pos, distance)) {
                val chunk = getOrCreateChunk(manager, pos)

                fun task() {
                    chunk.loadChunk(manager, data.level!!.getChunk(pos.x, pos.z))
                }

                if (VibrancyConfig.useMultithreading) {
                    sectionLoadTasks.add(VibrancyThreadPool.submit(data, pos, manager) {
                        task()
                        AutoCloseable { } to { }
                    })
                } else {
                    task()
                }

                return@removeIf true
            } else {
                return@removeIf false
            }
        }
        profiler.pop()

        profiler.push("finish")
        sectionLoadTasks.removeIf {
            if (it.isDoneOrCancelled()) {
                it.finish(profiler)
                true
            } else {
                false
            }
        }
        tasks.values.removeIf {
            if (it.isDoneOrCancelled()) {
                it.finish(profiler)
                true
            } else {
                false
            }
        }
        profiler.pop()

        profiler.push("loadDirty")
        for (section in manager.dirtySections) {
            chunks[section.first]?.dirty = true
        }
        profiler.pop()

        profiler.push("submit")
        chunks.forEach { (pos, chunk) ->
            if (chunk.dirty && manager.inGridRenderDistance(data, pos, distance)) {
                for (x in (pos.x - 1)..(pos.x + 1)) {
                    for (z in (pos.z - 1)..(pos.z + 1)) {
                        if (!data.level!!.hasChunk(x, z)) {
                            return@forEach
                        }

                        for (y in (pos.y - 1).coerceAtLeast(data.level!!.minSection)..(pos.y + 1).coerceAtMost(data.level!!.maxSection)) {
                            if (!scanned.contains(SectionPos.of(x, y, z))) {
                                return@forEach
                            }
                        }
                    }
                }

                println(chunk.x++)

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

                        val origin = NeoVec3i(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ())

                        val lights = chunk.map.values.toList()

                        profiler?.push("collect")

                        val lightList = arrayListOf<SubtleLight>()
                        val quads = arrayListOf<Pair<Pair<LightFace, Short>, Int>>()
                        val caches = hashMapOf<SectionPos, SectionMeshCache?>()
                        val chunkCache = ChunkSectionCache(data.level!!)
                        var lightIndex = 0
                        val consumer = object : BlockMeshCollector.Consumer {
                            override fun collect(
                                faces: Iterable<LightFace>,
                                section: SectionPos,
                                block: BlockPos,
                                translucent: Boolean
                            ) {
                                val offset = SectionPos.sectionRelativePos(block)
                                faces.mapTo(quads) { it to offset to lightIndex }
                            }
                        }

                        lights.forEach { light ->
                            if (isCancelled()) {
                                return AutoCloseable { } to { }
                            }

                            lightIndex = lightList.size
                            lightList.add(light)

                            chunkCache[light.boundingBox].forEach { (pos, state) ->
                                if (!light.pos.equals(pos.x, pos.y, pos.z)) {
                                    val otherLightInfo = BlockLightRegistry.get(state.block, SubtleLightType)

                                    if (otherLightInfo != null) {
                                        val otherLight = SubtleLight(otherLightInfo, state, NeoVec3i(pos))

                                        if (otherLight == light) {
                                            return@forEach
                                        }
                                    }
                                }

                                BlockMeshCollector.collectLightFaces(
                                    manager,
                                    caches,
                                    state,
                                    data.level!!,
                                    pos,
                                    consumer
                                )
                            }
                        }
                        profiler?.pop()

                        if (isCancelled()) {
                            return AutoCloseable { } to { }
                        }

                        profiler?.push("ssbo")
                        val buffer = NeoBuffer.GCNative(lightList.size.toLong() * 8 * Float.SIZE_BYTES)

                        buffer.write().run {
                            for (light in lightList) {
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
                                writeFloat(light.flicker)
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
                    tasks.put(pos, VibrancyThreadPool.submit(data, pos, manager) { impl(it, null) })?.cancel()
                } else {
                    val result = impl({ false }, profiler)
                    result.second()
                    result.first.close()
                }

                chunk.dirty = false
            }
        }
        profiler.pop()
    }

    inner class Chunk(
        @JvmField
        val pos: SectionPos
    ) : HashMapBlockLightStorage<SubtleLightInfo, SubtleLight>(SubtleLightType), NativeResource {
        @JvmField
        val mesh = Mesh(
            VERTEX_FORMAT,
            GlBeginMode.QUADS,
            GlBufferWriter.Mode.REGULAR,
            GlBufferUsage.STATIC_DRAW
        )
        @JvmField
        val ssbo = NeoGlBuffer()
        @JvmField
        var box: AbstractRect3<Int>? = null
        @JvmField
        var dirty = true
        var x = 0

        override fun shouldCollectMeshGeometry(pos: SectionPos): Boolean {
            return pos == this.pos
        }

        fun lazyUpload(isCancelled: () -> Boolean, quads: Collection<Pair<Pair<LightFace, Short>, Int>>): Pair<AutoCloseable, () -> Unit> {
            val vertexBuffer = NeoBuffer.GCNative(quads.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            vertexBuffer.write().run {
                quads.forEachIndexed { index, quad ->
                    if (isCancelled()) {
                        return vertexBuffer to { }
                    }

                    quad.first.first.apply { vertex ->
                        writeFloat(vertex.x + SectionPos.sectionRelativeX(quad.first.second))
                        writeFloat(vertex.y + SectionPos.sectionRelativeY(quad.first.second))
                        writeFloat(vertex.z + SectionPos.sectionRelativeZ(quad.first.second))

                        writeShort((vertex.u * 0xFFFF).toInt())
                        writeShort((vertex.v * 0xFFFF).toInt())

                        writeShort(quad.second)
                        writeInt(vertex.color)

                        writeByte(vertex.normal)
                        writeByte(vertex.normal ushr 8)
                        writeByte(vertex.normal ushr 16)
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
                && manager.isSectionVisible(pos)
            ) {
                debugOut("lightsRendered", size)
                debugOut("sectionsRendered", 1)

                profiler.push("transforms")
                val blockPos = NeoVec3i(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ())

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
                shader.setTexture(0, GlTextureBinding.FromInstance(
                    NeoAtlas.blocks,
                    GlTextureTarget.TEXTURE_2D
                ))
                shader.setShaderStorageBuffer("LightBuffer", ssbo)

                shader.setUniform("FlickerStrength") { set(VibrancyConfig.flickerStrength) }
                shader.setUniform("GLFWTime") { set(glfwGetTime().toFloat()) }
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
                                        NeoVec3i(x + origin.x, y + origin.y, z + origin.z),
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
            mesh.free()
            ssbo.free()
        }

        override fun clear(manager: LightManager) {
            super.clear(manager)
            dirty = true
        }
    }
}