package net.typho.vibrancy.block.impl

import dev.ryanhcode.sable.companion.SableCompanion
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.iterator
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.NeoVec3d
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.ChunkedBlockLightStorage
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.util.VibrancyThreadPool
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.concurrent.CompletableFuture

class SubtleLightStorage : ChunkedBlockLightStorage<SubtleLightInfo, SubtleLightStorage.Chunk>(SubtleLightType) {
    @JvmField
    val dirty = HashSet<ChunkPos>()
    @JvmField
    val tasks = LinkedList<CompletableFuture<() -> Unit>>()

    override fun createChunk(manager: LightManager, pos: ChunkPos): Chunk {
        return Chunk(pos)
    }

    internal fun markBlockDirty(chunk: ChunkPos, pos: IVec3<Int>) {
        dirty.add(chunk)
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
        data: RenderEventData,
        profiler: ProfilerFiller
    ) {
        profiler.push("finish")
        tasks.removeIf { task ->
            if (task.isDone) {
                task.get()()
                return@removeIf true
            } else {
                return@removeIf false
            }
        }
        profiler.pop()

        profiler.push("submit")
        synchronized(dirty) {
            for (pos in dirty) {
                val chunk = getOrCreateChunk(manager, pos)

                fun impl(profiler: ProfilerFiller?): () -> Unit {
                    synchronized(chunk.map) {
                        profiler?.push("fold")
                        chunk.box = chunk.map.values.fold(null) { box, light ->
                            box?.include(light.boundingBox) ?: light.boundingBox
                        }
                        profiler?.pop()

                        if (chunk.map.isEmpty()) {
                            return { }
                        }

                        val quads = arrayListOf<NeoBakedQuad>()
                        val origin = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)

                        profiler?.push("collect")
                        chunk.map.values.forEach { light ->
                            val lightPos = (light.pos - origin).toFloat() + light.offset

                            light.shadowBox.iterator().forEach { block ->
                                //if (
                                //    block.x >= pos.minBlockX && block.x <= pos.maxBlockX &&
                                //    block.z >= pos.minBlockZ && block.z <= pos.maxBlockZ
                                //) {
                                    BlockMeshCollector.collectLightFaces(
                                        manager,
                                        data.level!!.getBlockState(block.blockPos),
                                        data.level!!,
                                        block,
                                        block - origin,
                                        NeoAtlas.blocks,
                                        true,
                                        object : BlockMeshCollector.Consumer {
                                            override val predicate = object : BlockMeshCollector.Predicate {
                                                override fun shouldCastBlock(
                                                    level: Level,
                                                    pos: IVec3<Int>,
                                                    state: BlockState?
                                                ): Boolean {
                                                    return true
                                                }

                                                override fun shouldCastFace(
                                                    face: NeoDirection?,
                                                    level: Level,
                                                    pos: IVec3<Int>,
                                                    state: BlockState?
                                                ): Boolean {
                                                    return face == null || BlockUtil.INSTANCE.shouldRenderFace(
                                                        level,
                                                        pos,
                                                        face,
                                                        state ?: level.getBlockState(pos.blockPos)
                                                    )
                                                }
                                            }

                                            override fun collect(faces: Iterable<LightFace>) {
                                                faces.mapTo(quads) { face ->
                                                    face.applyTint { pos, color ->
                                                        val value = SubtleLight.sampleLight(lightPos, pos)
                                                        NeoColor.RGBAF(
                                                            /*color.r * color.a * */light.color.r,
                                                            /*color.g * color.a * */light.color.g,
                                                            /*color.b * color.a * */light.color.b,
                                                            value
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                //}
                            }
                        }
                        profiler?.pop()

                        profiler?.push("upload")
                        val task = chunk.mesh.lazyUploadNoAtlas(quads)
                        profiler?.pop()

                        return task
                    }
                }

                if (VibrancyConfig.useMultithreading) {
                    tasks.add(VibrancyThreadPool.submit(data, pos, manager) { impl(null) })
                } else {
                    impl(profiler)()
                }
            }

            dirty.clear()
        }
        profiler.pop()
    }

    inner class Chunk(
        @JvmField
        val pos: ChunkPos
    ) : HashMapBlockLightStorage<SubtleLightInfo, SubtleLight>(SubtleLightType), NativeResource {
        @JvmField
        var box: AbstractRect3<Int>? = null
        @JvmField
        val mesh = LightMesh(GlBufferUsage.STATIC_DRAW)

        fun scan(level: Level, manager: LightManager) {
            map.clear()

            level.getChunk(pos.x, pos.z).findBlocks(BlockLightRegistry::has) { pos, state ->
                BlockLightRegistry.get(state.block, SubtleLightType)?.let { info ->
                    addLight(manager, level, state, NeoVec3i(pos), info)
                }
            }
        }

        fun render(manager: LightManager, data: RenderEventData, shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit) {
            if (
                size > 0
                && box?.let { manager.testFrustum(pos, data, it) } ?: true
            ) {
                val blockPos = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)
                val subLevel = SableCompanion.INSTANCE.getContainingClient(pos)

                if (subLevel == null) {
                    shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((blockPos.toFloat() - data.camera.pos).toJOML(), Matrix4f())) }
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
                }

                //shader.setTexture(1, GlTextureBinding.FromInstance(lightTexture, GlTextureTarget.TEXTURE_2D))
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
        }
    }
}