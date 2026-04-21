package net.typho.vibrancy

import dev.ryanhcode.sable.companion.ClientSubLevelAccess
import dev.ryanhcode.sable.companion.SableCompanion
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.NeoVec3d
import net.typho.big_shot_lib.api.util.resource.NeoResourceKey
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.BlockLightStorage
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.sky.SkyLightRegistry
import net.typho.vibrancy.sky.SkyLightStorage
import net.typho.vibrancy.sky.SkyLightType
import java.util.*
import java.util.function.Consumer

open class LightManager {
    @JvmField
    val dirtyBlocks = LinkedList<IVec3<Int>>()
    @JvmField
    val blockLights = HashMap<BlockLightType<*, *>, BlockLightStorage<*>>()
    @JvmField
    var skyLight: Pair<SkyLightType<*, *>, SkyLightStorage<*>>? = null
    @JvmField
    protected val debugInfo = HashMap<NeoResourceKey<*>?, HashMap<String, Int>>()

    fun getLevel(): ClientLevel? = Minecraft.getInstance().level

    fun clear() {
        blockLights.values.forEach { storage -> storage.clear(this) }
        skyLight?.second?.clear(this)
    }

    fun reload() {
        if (VibrancyConfig.modEnabled) {
            for (light in blockLights.values) {
                light.reload(this, null)
            }

            skyLight?.second?.reload(this)
        }
    }

    fun ensureStorageInitialized() {
        for (type in BlockLightRegistry.registry!!.values()) {
            blockLights.computeIfAbsent(type) { type -> type.createStorage(this) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <I : BlockLightInfo> addBlockLight(
        pos: IVec3<Int>,
        level: Level,
        state: BlockState,
        type: BlockLightType<I, *>,
        info: Any
    ) {
        (blockLights[type] as BlockLightStorage<I>).addLight(this, level, state, pos, info as I)
    }

    fun blockChanged(
        level: Level,
        pos: IVec3<Int>,
        old: BlockState,
        new: BlockState
    ) {
        ensureStorageInitialized()

        for (entry in blockLights) {
            entry.value.removeLight(this, level, pos)

            BlockLightRegistry.get(new.block, entry.key)?.let { addBlockLight(pos, level, new, entry.key, it) }
        }

        dirtyBlocks.add(pos)
    }

    fun loadChunk(chunk: ChunkAccess) {
        ensureStorageInitialized()

        blockLights.values.forEach { storage -> storage.loadChunk(this, chunk) }
        skyLight?.second?.loadChunk(this, chunk)

        for (light in blockLights.values) {
            light.reload(this, chunk.pos)
        }
    }

    fun deloadChunk(chunk: ChunkAccess) {
        ensureStorageInitialized()

        blockLights.values.forEach { storage -> storage.deloadChunk(this, chunk) }
        skyLight?.second?.deloadChunk(this, chunk)

        for (light in blockLights.values) {
            light.reload(this, chunk.pos)
        }
    }

    protected fun getDebugOutput(key: NeoResourceKey<*>?): (String, Int) -> Unit {
        val debugMap = debugInfo.computeIfAbsent(key) { HashMap() }
        return { key, value -> debugMap.compute(key) { k, v -> if (v == null) value else v + value } }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <S : BlockLightStorage<*>> castAndRender(data: RenderEventData, type: BlockLightType<*, S>, storage: BlockLightStorage<*>) {
        type.render(this, data, storage as S, getDebugOutput(BlockLightRegistry.registry!!.getKey(type)))
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <S : SkyLightStorage<*>> castAndRender(data: RenderEventData, type: SkyLightType<*, S>, storage: SkyLightStorage<*>) {
        type.render(this, data, storage as S, getDebugOutput(SkyLightRegistry.registry!!.getKey(type)))
    }

    fun render(data: RenderEventData) {
        debugInfo.clear()

        for (entry in blockLights) {
            castAndRender(data, entry.key, entry.value)
        }

        skyLight?.let { castAndRender(data, it.first, it.second) }

        dirtyBlocks.clear()
    }

    fun testFrustum(origin: IVec3<Int>, data: RenderEventData, box: AbstractRect3<Int>): Boolean {
        return testFrustum(SableCompanion.INSTANCE.getContainingClient(origin.toDouble().toJOML()), data, box)
    }

    fun testFrustum(origin: ChunkPos, data: RenderEventData, box: AbstractRect3<Int>): Boolean {
        return testFrustum(SableCompanion.INSTANCE.getContainingClient(origin), data, box)
    }

    fun testFrustum(subLevel: ClientSubLevelAccess?, data: RenderEventData, box: AbstractRect3<Int>): Boolean {
        if (subLevel == null) {
            return data.frustum.testAab(
                (box.min.toFloat() - data.camera.pos).toJOML(),
                (box.max.toFloat() - data.camera.pos).toJOML(),
            )
        } else {
            val box = subLevel.boundingBox()
            return data.frustum.testAab(
                (NeoVec3d(box.minX(), box.minY(), box.minZ()).toFloat() - data.camera.pos).toJOML(),
                (NeoVec3d(box.maxX(), box.maxY(), box.maxZ()).toFloat() - data.camera.pos).toJOML(),
            )
        }
    }

    fun getDebugOutput(out: Consumer<String>) {
        debugInfo[null]?.forEach { (key, value) -> out.accept("$key: $value") }

        for (entry in blockLights) {
            val key = BlockLightRegistry.registry!!.getKey(entry.key)
            out.accept(ChatFormatting.UNDERLINE.toString() + key.location.toString())
            out.accept("lightsInWorld: ${entry.value.size}")

            debugInfo[key]?.forEach { (key, value) -> out.accept("$key: $value") }
        }

        skyLight?.let {
            val key = SkyLightRegistry.registry!!.getKey(it.first)
            out.accept(ChatFormatting.UNDERLINE.toString() + "Sky Light: " + key.location.toString())
            debugInfo[key]?.forEach { (key, value) -> out.accept("$key: $value") }
        }
    }

    fun clampToChunkRenderDistance(distance: Int): Int {
        return distance.coerceAtMost(Minecraft.getInstance().options.effectiveRenderDistance)
    }

    fun inRenderDistance(testDistanceSquared: Float, renderDistance: Int): Boolean {
        val x = clampToChunkRenderDistance(renderDistance) * 16f
        return testDistanceSquared <= x * x
    }

    fun inRenderDistance(data: RenderEventData, pos: ChunkPos, distance: Int): Boolean {
        val subLevel = SableCompanion.INSTANCE.getContainingClient(pos)

        return if (subLevel == null) {
            data.camera.pos.xz.inDistance(pos.middleBlockX.toFloat(), pos.middleBlockZ.toFloat(), clampToChunkRenderDistance(distance) * 16f)
        } else {
            data.camera.pos.inDistance(NeoVec3d(subLevel.renderPose().position()).toFloat(), clampToChunkRenderDistance(distance) * 16f)
        }
    }

    fun getSortingOrder(data: RenderEventData, pos: IVec3<Int>): Float {
        val a = pos.toFloat() + 0.5f
        val b = data.camera.pos
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(data.level!!, a.x.toDouble(), a.y.toDouble(), a.z.toDouble(), b.x.toDouble(), b.y.toDouble(), b.z.toDouble()).toFloat()
    }
}