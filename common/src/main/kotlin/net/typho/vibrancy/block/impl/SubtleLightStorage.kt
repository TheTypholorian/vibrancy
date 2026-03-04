package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferType
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.GlBuffer
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.util.MeshUtil
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy.toBlockBox
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.ChunkedBlockLightStorage
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.shadows.BasicShadowMesher
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.ShadowPredicate
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

    override fun createChunk(pos: ChunkPos) = Chunk(pos)

    override fun clear(manager: LightManager) {
        super.clear(manager)
        synchronized(dirty) {
            dirty.clear()
        }
    }

    override fun reload(manager: LightManager) {
        super.reload(manager)
        synchronized(dirty) {
            dirty.addAll(chunks.keys)
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

        val level = manager.getLevel() ?: return

        synchronized(dirty) {
            for (pos in dirty) {
                val newChunk = createChunk(pos)
                val atlas = TextureUtil.INSTANCE.getTextureAtlasDimensions(TextureUtil.INSTANCE.blockAtlasId)

                tasks.add(
                    CompletableFuture.supplyAsync {
                        val chunk = level.getChunk(pos.x, pos.z)
                        val box = BlockBox(
                            BlockPos(pos.minBlockX - 1, chunk.minBuildHeight, pos.minBlockZ - 1),
                            BlockPos(pos.maxBlockX + 1, chunk.maxBuildHeight, pos.maxBlockZ + 1),
                        )

                        for (x in pos.x - 1..pos.x + 1) {
                            for (z in pos.z - 1..pos.z + 1) {
                                level.getChunk(x, z).findBlocks({ BlockLightRegistry.has(it.block) }) { pos, state ->
                                    val actualPos = BlockPos(pos)

                                    if (box.contains(actualPos)) {
                                        BlockLightRegistry.get(state.block, SubtleLightType)?.let { info ->
                                            newChunk.map[actualPos] = newChunk.createLight(manager, state, actualPos, info)
                                        }
                                    }
                                }
                            }
                        }

                        if (newChunk.map.isEmpty()) {
                            return@supplyAsync null
                        }

                        val blocks = HashSet<BlockPos>()
                        val ssboBuffer = MemoryUtil.memAllocFloat(8 * newChunk.size)

                        for (light in newChunk.map.values) {
                            if (light.shouldRender(newChunk)) {
                                blocks.addAll(
                                    light.boundingBox.toBlockBox()
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

                        val mesher = BasicShadowMesher()
                        val predicate = object : ShadowPredicate {
                            override fun shouldCastBlock(
                                block: BlockState,
                                level: Level,
                                pos: BlockPos
                            ): Boolean {
                                return true
                            }

                            override fun shouldCastFluid(
                                fluid: FluidState,
                                level: Level,
                                pos: BlockPos
                            ): Boolean {
                                return true
                            }

                            override fun shouldCastFace(
                                face: Direction?,
                                state: BlockState,
                                level: Level,
                                pos: BlockPos
                            ): Boolean {
                                if (face == null) {
                                    return true
                                }

                                val sidePos = pos.relative(face)

                                if (newChunk.map.containsKey(pos) || newChunk.map.containsKey(sidePos)) {
                                    return true
                                }

                                val sideState = level.getBlockState(sidePos)

                                if (BlockUtil.INSTANCE.isSolidRender(state, pos, level) && BlockUtil.INSTANCE.isSolidRender(sideState, sidePos, level)) {
                                    return false
                                }

                                //if (
                                //    newChunk.map.keys.filter { it.distSqr(pos) <= 4 }
                                //        .none { face.step().dot(it.center.subtract(pos.center).toVector3f()) > 0 }
                                //) {
                                //    return false
                                //}

                                return true
                            }

                            override fun isInLightRange(pos: BlockPos): Boolean {
                                return true
                            }

                            override fun isInShadowRange(pos: BlockPos): Boolean {
                                return false
                            }
                        }

                        for (pos in blocks) {
                            mesher.submit(
                                manager,
                                level,
                                pos,
                                RandomSource.create(),
                                predicate
                            )
                        }

                        val faces = LinkedList<LightFace>()
                        mesher.finish(manager, predicate, level, {}, faces::add)
                        val task = newChunk.mesh.build(level, faces, atlas.width, atlas.height)

                        return@supplyAsync Consumer { data ->
                            task.run()
                            newChunk.ssbo.upload(ssboBuffer.flip())
                            MemoryUtil.memFree(ssboBuffer)

                            chunks.put(pos, newChunk)?.free()

                            val blitSettings = SubtleLightType.meshBlitSettings(data, newChunk)
                            blitSettings.bind()
                            MeshUtil.SCREEN_MESH.draw()
                            blitSettings.unbind()
                        }
                    }
                )
            }

            dirty.clear()
        }
    }

    inner class Chunk(
        @JvmField
        val pos: ChunkPos,
        @JvmField
        val mesh: LightMesh = LightMesh(),
        @JvmField
        val ssbo: GlBuffer = GlBuffer(BufferType.SHADER_STORAGE_BUFFER, BufferUsage.STATIC_DRAW)
    ) : HashMapBlockLightStorage<SubtleLightInfo, SubtleLight>(SubtleLightType), NativeResource {
        val box: AABB?
            get() = map.values.fold(null) { box, light -> if (box == null) light.boundingBox else box.minmax(light.boundingBox) }

        fun render(fbo: GlFramebuffer, data: RenderEventData): LightRenderResult {
            if (
                size > 0
                //&& box?.let { data.frustum.testAab(it.minPosition.toVector3f(), it.maxPosition.toVector3f()) } ?: true
            ) {
                mesh.draw(fbo, data, TextureUtil.INSTANCE.blockAtlas)

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
        ): SubtleLight {
            return SubtleLight(info, state, pos)
        }

        override fun addLight(manager: LightManager, state: BlockState, pos: BlockPos, info: SubtleLightInfo) {
            super.addLight(manager, state, pos, info)
            synchronized(dirty) {
                dirty.add(ChunkPos(pos))
            }
        }

        override fun removeLight(manager: LightManager, pos: BlockPos) {
            super.removeLight(manager, pos)
            synchronized(dirty) {
                dirty.add(ChunkPos(pos))
            }
        }

        override fun reload(manager: LightManager) {
        }

        override fun free() {
            mesh.free()
            ssbo.free()
        }
    }
}