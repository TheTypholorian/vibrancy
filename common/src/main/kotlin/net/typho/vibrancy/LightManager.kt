package net.typho.vibrancy

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.BlockLightStorage
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.sky.SkyLightStorage
import net.typho.vibrancy.sky.SkyLightType
import org.joml.Vector2f
import java.util.*
import java.util.function.Consumer

open class LightManager {
    @JvmField
    val dirtyBlocks = LinkedList<GlobalPos>()
    @JvmField
    val blockLights = HashMap<BlockLightType<*, *>, BlockLightStorage<*>>()
    @JvmField
    var blockRenderResults = HashMap<BlockLightType<*, *>, LightRenderResult>()
    @JvmField
    var skyLight: Pair<SkyLightType<*, *>, SkyLightStorage<*>>? = null
    @JvmField
    var skyRenderResult: LightRenderResult? = null

    fun getLevel(): ClientLevel? = Minecraft.getInstance().level

    fun clear() {
        blockLights.values.forEach { storage -> storage.clear(this) }
        skyLight?.second?.clear(this)
    }

    fun reload() {
        for (light in blockLights.values) {
            light.reload(this, null)
        }

        skyLight?.second?.reload(this)
    }

    fun ensureStorageInitialized() {
        for (type in BlockLightRegistry.registry!!.values()) {
            blockLights.computeIfAbsent(type) { type -> type.createStorage(this) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <I : BlockLightInfo> addBlockLight(
        pos: BlockPos,
        level: Level,
        state: BlockState,
        type: BlockLightType<I, *>,
        info: Any
    ) {
        (blockLights[type] as BlockLightStorage<I>).addLight(this, level, state, pos, info as I)
    }

    fun blockChanged(
        level: Level,
        pos: BlockPos,
        old: BlockState,
        new: BlockState
    ) {
        ensureStorageInitialized()

        for (entry in blockLights) {
            entry.value.removeLight(this, level, pos)

            BlockLightRegistry.get(new.block, entry.key)?.let { addBlockLight(pos, level, new, entry.key, it) }
        }

        dirtyBlocks.add(GlobalPos(level.dimension(), pos))
    }

    fun loadChunk(chunk: LevelChunk) {
        ensureStorageInitialized()

        blockLights.values.forEach { storage -> storage.loadChunk(this, chunk) }
        skyLight?.second?.loadChunk(this, chunk)

        for (light in blockLights.values) {
            for (x in chunk.pos.x - 1..chunk.pos.x + 1) {
                for (z in chunk.pos.z - 1..chunk.pos.z + 1) {
                    light.reload(this, ChunkPos(x, z))
                }
            }
        }
    }

    fun deloadChunk(chunk: LevelChunk) {
        ensureStorageInitialized()

        blockLights.values.forEach { storage -> storage.deloadChunk(this, chunk) }
        skyLight?.second?.deloadChunk(this, chunk)

        for (light in blockLights.values) {
            for (x in chunk.pos.x - 1..chunk.pos.x + 1) {
                for (z in chunk.pos.z - 1..chunk.pos.z + 1) {
                    light.reload(this, ChunkPos(x, z))
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <S : BlockLightStorage<*>> castAndRender(data: RenderEventData, fbo: GlFramebuffer, type: BlockLightType<*, S>, storage: BlockLightStorage<*>): LightRenderResult {
        return type.render(this, data, storage as S, fbo)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <S : SkyLightStorage<*>> castAndRender(data: RenderEventData, fbo: GlFramebuffer, type: SkyLightType<*, S>, storage: SkyLightStorage<*>): LightRenderResult {
        return type.render(this, data, storage as S, fbo)
    }

    fun render(data: RenderEventData, fbo: GlFramebuffer) {
        blockRenderResults.clear()

        for (entry in blockLights) {
            blockRenderResults[entry.key] = castAndRender(data, fbo, entry.key, entry.value)
        }

        skyRenderResult = skyLight?.let { castAndRender(data, fbo, it.first, it.second) }

        dirtyBlocks.clear()
    }

    fun getDebugOutput(out: Consumer<String>) {
        for (entry in blockLights) {
            out.accept(ChatFormatting.UNDERLINE.toString() + BlockLightRegistry.registry!!.getKey(entry.key).location.toString())
            out.accept("${entry.value.size} lights in world")

            blockRenderResults[entry.key]?.accept(out)
        }
    }

    fun clampToChunkRenderDistance(distance: Int): Int {
        return distance.coerceAtMost(Minecraft.getInstance().options.effectiveRenderDistance)
    }

    fun inRenderDistance(data: RenderEventData, pos: BlockPos, distance: Int): Boolean {
        val d = clampToChunkRenderDistance(distance)
        return pos.center.toVector3f().distanceSquared(data.camera.pos) <= d * d * 16 * 16
    }

    fun inRenderDistance(data: RenderEventData, pos: ChunkPos, distance: Int): Boolean {
        val centerChunk = Vector2f(pos.middleBlockX.toFloat(), pos.middleBlockZ.toFloat())
        val d = clampToChunkRenderDistance(distance)
        return centerChunk.distanceSquared(Vector2f(data.camera.pos.x, data.camera.pos.z)) <= d * d * 16 * 16
    }

    fun getSortingOrder(data: RenderEventData, pos: BlockPos): Float {
        return pos.center.toVector3f().distanceSquared(data.camera.pos)
    }

    fun getSortingOrder(data: RenderEventData, pos: ChunkPos): Float {
        return Vector2f(pos.x.toFloat(), pos.z.toFloat()).distanceSquared(Vector2f(data.camera.pos.x / 16, data.camera.pos.z / 16))
    }
}