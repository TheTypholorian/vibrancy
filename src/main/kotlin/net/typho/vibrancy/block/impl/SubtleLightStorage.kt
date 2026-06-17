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
import net.typho.vibrancy.shadows.PositionedLightFace
import net.typho.vibrancy.util.ChunkSectionCache
import net.typho.vibrancy.util.GlTask
import net.typho.vibrancy.util.SectionMeshCache
import net.typho.vibrancy.util.VibrancyThreadPool
import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.system.NativeResource
import java.util.function.Consumer
import kotlin.use

class SubtleLightStorage : SectionedBlockLightStorage<SubtleLightInfo, SubtleLightStorage.Chunk>(SubtleLightType) {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", LightMesh.POSITION_3)
            .add("UV0", LightMesh.TEXTURE_UV_3)
            .add("LightIndex", LightMesh.LIGHT_INDEX)
            /*
            .add("Position", NeoVertexFormat.Element.POSITION) // 12 bytes
            .add("UV0", LightMesh.COMPACT_TEXTURE_UV) // 4 bytes
            .add("LightIndex", LightMesh.LIGHT_INDEX) // 2 bytes
            .add("Color", NeoVertexFormat.Element.COLOR) // 4 bytes
            .add("Normal", NeoVertexFormat.Element.NORMAL) // 3 bytes
             */
            .build()
    }

    @JvmField
    val dirty = hashSetOf<Chunk>()
    @JvmField
    val tasks = hashMapOf<SectionPos, GlTask<Unit>>()
    @JvmField
    val sectionLoadQueue = hashSetOf<SectionPos>()

    override fun createChunk(manager: LightManager, pos: SectionPos): Chunk {
        return Chunk(pos)
    }

    override fun loadSection(manager: LightManager, chunk: ChunkAccess, pos: SectionPos) {
        val section = chunk.getSection(chunk.getSectionIndexFromSectionY(pos.y))

        if (section.maybeHas { BlockLightRegistry.get(it.block, SubtleLightType) != null }) {
            sectionLoadQueue.add(pos)
        }
    }

    fun checkDirty(
        manager: LightManager,
        data: RenderEventData,
        profiler: ProfilerFiller
    ) {
        val distance = manager.getGridRenderDistance(VibrancyConfig.subtleLightsRenderDistance).toFloat()

        sectionLoadQueue.removeIf { pos ->
            if (manager.inGridRenderDistance(data, pos, distance)) {
                getOrCreateChunk(manager, pos).loadChunk(manager, data.level!!.getChunk(pos.x, pos.z))

                return@removeIf true
            } else {
                return@removeIf false
            }
        }

        profiler.push("finish")
        tasks.values.removeIf {
            if (it.isDoneOrCancelled()) {
                it.finish()
                true
            } else {
                false
            }
        }
        profiler.pop()

        profiler.push("loadDirty")
        for (section in manager.dirtySections) {
            chunks[section.first]?.let {
                dirty.add(it)
            }
        }
        profiler.pop()

        profiler.push("submit")
        dirty.removeIf { chunk ->
            if (chunk.isCompiledEmpty && chunk.map.isEmpty()) {
                return@removeIf true
            }

            for (x in (chunk.pos.x - 1)..(chunk.pos.x + 1)) {
                for (z in (chunk.pos.z - 1)..(chunk.pos.z + 1)) {
                    if (!data.level!!.hasChunk(x, z)) {
                        return@removeIf false
                    }
                }
            }

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

                    val lights = chunk.map.values.toList()

                    profiler?.push("collect")
                    val builder = chunk.builder()
                    val caches = hashMapOf<SectionPos, SectionMeshCache?>()
                    val chunkCache = ChunkSectionCache(data.level!!)

                    lights.forEach { light ->
                        if (isCancelled()) {
                            return AutoCloseable { } to { }
                        }

                        builder.light(light) { builder ->
                            chunkCache[light.shadowBox].forEach { (pos, state) ->
                                BlockMeshCollector.collectLightFaces(
                                    manager,
                                    caches,
                                    state,
                                    data.level!!,
                                    pos,
                                    builder
                                )
                            }
                        }
                    }
                    profiler?.pop()

                    if (isCancelled()) {
                        return AutoCloseable { } to { }
                    }

                    profiler?.push("upload")
                    val task = builder.build(isCancelled)
                    profiler?.pop()

                    return task
                }
            }

            if (VibrancyConfig.useMultithreading) {
                tasks.put(chunk.pos, VibrancyThreadPool.submit(data, chunk.pos, manager) { impl(it, null) })?.cancel()
            } else {
                val result = impl({ false }, profiler)
                result.second()
                result.first.close()
            }

            return@removeIf true
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
        var isCompiledEmpty = true
        @JvmField
        var box: AbstractRect3<Int>? = null

        override fun shouldCollectMeshGeometry(pos: SectionPos): Boolean {
            return pos == this.pos
        }

        inner class Builder {
            private val lights = arrayListOf<LightBuilder>()

            inner class LightBuilder(
                @JvmField
                val light: SubtleLight
            ) : BlockMeshCollector.Consumer {
                @JvmField
                val faces = arrayListOf<PositionedLightFace>()

                override fun collect(
                    faces: Iterable<LightFace>,
                    section: SectionPos,
                    block: BlockPos,
                    translucent: Boolean
                ) {
                    faces.mapTo(this.faces) { it.positioned(block) }
                }
            }

            fun light(light: SubtleLight, out: Consumer<LightBuilder>): Builder {
                val builder = LightBuilder(light)
                out.accept(builder)
                lights.add(builder)
                return this
            }

            fun build(isCancelled: () -> Boolean): Pair<AutoCloseable, () -> Unit> {
                if (lights.isEmpty()) {
                    isCompiledEmpty = true
                    return AutoCloseable { } to { }
                } else {
                    isCompiledEmpty = false

                    val numFaces = lights.sumOf { it.faces.size }
                    val vertexBuffer = NeoBuffer.GCNative(numFaces.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

                    vertexBuffer.write().run {
                        lights.forEachIndexed { lightIndex, light ->
                            if (isCancelled()) {
                                return vertexBuffer to { }
                            }

                            light.faces.forEach { face ->
                                val offsetX = face.block.x - light.light.offset.x
                                val offsetY = face.block.y - light.light.offset.y
                                val offsetZ = face.block.z - light.light.offset.z

                                face.apply { vertex ->
                                    val data = (((vertex.x + offsetX + 2) * 64).toLong() shl 56) or
                                            (((vertex.y + offsetY + 2) * 64).toLong() shl 48) or
                                            (((vertex.z + offsetZ + 2) * 64).toLong() shl 40) or
                                            ((vertex.u * 4095).toLong() shl 28) or
                                            ((vertex.v * 4095).toLong() shl 16) or
                                            lightIndex.toLong()

                                    writeLong(java.lang.Long.reverseBytes(data))
                                }
                            }
                        }
                    }

                    val lightBuffer = NeoBuffer.GCNative(lights.size.toLong() * 32)
                    val origin = NeoVec3i(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ())

                    lightBuffer.write().run {
                        lights.forEach { light ->
                            if (isCancelled()) {
                                return AutoCloseable {
                                    vertexBuffer.free()
                                    lightBuffer.free()
                                } to { }
                            }

                            val color = light.light.color
                            val pos = (light.light.pos - origin).toFloat() + light.light.offset

                            writeFloat(pos.x)
                            writeFloat(pos.y)
                            writeFloat(pos.z)
                            writeInt(light.light.shape)

                            writeFloat(color.x)
                            writeFloat(color.y)
                            writeFloat(color.z)
                            writeFloat(light.light.flicker)
                        }
                    }

                    val indices = mesh.generateIndices(numFaces * 4)

                    return AutoCloseable {
                        vertexBuffer.free()
                        indices.first.free()
                        lightBuffer.free()
                    } to {
                        mesh.rawUpload(numFaces * 6, indices.second, vertexBuffer, indices.first)

                        ssbo.bind(GlBufferTarget.SHADER_STORAGE_BUFFER).use {
                            it.bufferData(lightBuffer, GlBufferUsage.STATIC_DRAW)
                        }
                    }
                }
            }
        }

        fun builder() = Builder()

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

            if (section.maybeHas(BlockLightRegistry::has)) {
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

            dirty.add(this)
        }

        override fun deloadChunk(manager: LightManager, chunk: ChunkAccess) {
            super.deloadChunk(manager, chunk)
            dirty.add(this)
        }

        override fun addLight(
            manager: LightManager,
            level: Level,
            state: BlockState,
            pos: IVec3<Int>,
            info: SubtleLightInfo
        ) {
            super.addLight(manager, level, state, pos, info)
            dirty.add(this)
        }

        override fun removeLight(manager: LightManager, level: Level, pos: IVec3<Int>): Boolean {
            if (super.removeLight(manager, level, pos)) {
                dirty.add(this)
                return true
            } else {
                return false
            }
        }

        override fun reload(manager: LightManager, chunk: ChunkPos?) {
            dirty.add(this)
        }

        override fun free() {
            tasks[pos]?.cancel()
            mesh.free()
            ssbo.free()
        }

        override fun clear(manager: LightManager) {
            super.clear(manager)
            dirty.add(this)
        }
    }
}