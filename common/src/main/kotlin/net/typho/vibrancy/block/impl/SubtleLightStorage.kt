package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferType
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.GlBuffer
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
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
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.concurrent.CompletableFuture

class SubtleLightStorage : ChunkedBlockLightStorage<SubtleLightInfo, SubtleLightStorage.Chunk>(SubtleLightType) {
    @JvmField
    val dirty = HashSet<ChunkPos>()
    @JvmField
    val tasks = LinkedList<CompletableFuture<Runnable?>>()

    override fun createChunk(pos: ChunkPos) = Chunk(pos)

    override fun clear(manager: LightManager) {
        super.clear(manager)
        dirty.clear()
    }

    override fun reload(manager: LightManager) {
        super.reload(manager)
        dirty.addAll(chunks.keys)
    }

    override fun loadChunk(manager: LightManager, chunk: LevelChunk) {
        super.loadChunk(manager, chunk)
        dirty.add(chunk.pos)
    }

    override fun deloadChunk(manager: LightManager, chunk: LevelChunk) {
        super.deloadChunk(manager, chunk)
        dirty.add(chunk.pos)
    }

    fun checkDirty(
        manager: LightManager
    ) {
        tasks.removeIf { task ->
            if (task.isDone) {
                task.get()?.run()
                return@removeIf true
            } else {
                return@removeIf false
            }
        }

        val level = manager.getLevel() ?: return

        for (pos in dirty) {
            val newChunk = createChunk(pos)
            val atlas = TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture)

            atlas.bind()

            val width = glGetTexLevelParameteri(atlas.type.glId, 0, GL_TEXTURE_WIDTH)
            val height = glGetTexLevelParameteri(atlas.type.glId, 0, GL_TEXTURE_HEIGHT)

            atlas.unbind()

            tasks.add(
                CompletableFuture.supplyAsync {
                    level.getChunk(pos.x, pos.z)
                        .findBlocks({ BlockLightRegistry.has(it.block) }) { pos, state ->
                            val actualPos = BlockPos(pos)

                            BlockLightRegistry.get(state.block, SubtleLightType)?.let { info ->
                                newChunk.addLight(manager, state, actualPos, info)
                            }
                        }

                    if (newChunk.map.isEmpty()) {
                        return@supplyAsync null
                    }

                    val blocks = HashSet<BlockPos>()
                    val ssboBuffer = MemoryUtil.memAllocFloat(8 * newChunk.size)

                    for (light in newChunk.map.values) {
                        blocks.addAll(light.boundingBox.toBlockBox().map { BlockPos(it) })

                        val color = light.color
                        val pos = light.absolutePos

                        ssboBuffer.put(color.x).put(color.y).put(color.z).put(0f)
                        ssboBuffer.put(pos.x).put(pos.y).put(pos.z).put(0f)
                    }

                    val mesher = BasicShadowMesher()
                    val predicate = object : ShadowPredicate {
                        override fun shouldCastBlock(
                            state: BlockState,
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
                            level.getBlockState(pos),
                            level,
                            pos,
                            RandomSource.create(),
                            predicate
                        )
                    }

                    val faces = LinkedList<LightFace>()
                    mesher.finish(manager, predicate, level, {}, faces::add)
                    val task = newChunk.mesh.build(faces, width, height)

                    return@supplyAsync Runnable {
                        task.run()
                        newChunk.ssbo.upload(ssboBuffer.flip())
                        MemoryUtil.memFree(ssboBuffer)

                        chunks.put(pos, newChunk)?.free()
                    }
                }
            )
        }

        dirty.clear()
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
                && box?.let { data.frustum.testAab(it.minPosition.toVector3f(), it.maxPosition.toVector3f()) } ?: true
            ) {
                mesh.draw(fbo, data, TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture))

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
            dirty.add(ChunkPos(pos))
        }

        override fun removeLight(manager: LightManager, pos: BlockPos) {
            super.removeLight(manager, pos)
            dirty.add(ChunkPos(pos))
        }

        override fun reload(manager: LightManager) {
        }

        override fun free() {
            mesh.free()
            ssbo.free()
        }
    }
}