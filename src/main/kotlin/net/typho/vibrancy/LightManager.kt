package net.typho.vibrancy

//? if sable {
/*import dev.ryanhcode.sable.companion.ClientSubLevelAccess
import dev.ryanhcode.sable.companion.SableCompanion
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension
*///? }

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.math.IRect3
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.block.BlockLightInfoLoader
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.BlockLightStorage
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightInfoLoader
import net.typho.vibrancy.sky.SkyLightRegistry
import net.typho.vibrancy.sky.SkyLightStorage
import net.typho.vibrancy.sky.SkyLightType
import net.typho.vibrancy.util.SectionMeshCache
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.function.Consumer

//? if >=1.21.5 {

//? }

open class LightManager {
    @JvmField
    val sectionLock = Any()
    @JvmField
    var nextDirtySections: MutableList<Pair<SectionPos, IRect3<Int>>> = LinkedList()
    @JvmField
    var dirtySections: MutableList<Pair<SectionPos, IRect3<Int>>> = LinkedList()
    @JvmField
    var dirtyBlocks: MutableMap<BlockPos, Pair<BlockState, BlockState>> = hashMapOf()
    @JvmField
    val blockLights = HashMap<BlockLightType<*, *>, BlockLightStorage<*>>()
    @JvmField
    var skyLight: Pair<SkyLightType<*, *>, SkyLightStorage<*>>? = null
    @JvmField
    protected val debugInfo = HashMap<Identifier?, HashMap<String, Int>>()
    @JvmField
    val sectionMeshCaches = hashMapOf<SectionPos, SectionMeshCache>()

    fun getLevel(): ClientLevel? = Minecraft.getInstance().level

    fun clear() {
        blockLights.values.forEach { storage -> storage.clear(this) }
        skyLight?.second?.clear(this)

        synchronized(sectionLock) {
            sectionMeshCaches.clear()
        }
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
        for (type in BlockLightRegistry.registry) {
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

        dirtyBlocks[pos.toBlockPos()] = old to new
    }

    fun levelChanged(
        old: ClientLevel?,
        new: ClientLevel?
    ) {
        clear()

        if (new == null) {
            (skyLight?.second as? NativeResource)?.free()
            skyLight = null
        } else {
            BlockLightInfoLoader.onResourceManagerReload(Minecraft.getInstance().resourceManager)
            SkyLightInfoLoader.onResourceManagerReload(Minecraft.getInstance().resourceManager)

            SkyLightRegistry.get(new)?.let { info ->
                if (skyLight?.first != info.type) {
                    (skyLight?.second as? NativeResource)?.free()
                    skyLight = null
                }

                if (skyLight == null) {
                    skyLight = info.type to info.type.createStorage(this)
                }

                @Suppress("UNCHECKED_CAST")
                fun <I : SkyLightInfo> load(storage: SkyLightStorage<I>) {
                    storage.load(this, info as I)
                }

                load(skyLight!!.second)
            }
        }
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

    protected fun getDebugOutput(key: Identifier?): (String, Int) -> Unit {
        if (!Minecraft.getInstance().debugOverlay.showDebugScreen()) {
            return { key, value -> }
        }

        val debugMap = debugInfo.computeIfAbsent(key) { HashMap() }
        return { key, value -> debugMap.compute(key) { k, v -> if (v == null) value else v + value } }
    }

    fun preRender() {
        debugInfo.clear()

        synchronized(sectionLock) {
            dirtySections = nextDirtySections
            nextDirtySections = LinkedList()
        }
    }

    fun postRender() {
        dirtySections.clear()
        dirtyBlocks.clear()

        blockLights.values.forEach { it.endFrame(this) }
    }

    /*
    @Suppress("UNCHECKED_CAST")
    protected fun <S : BlockLightStorage<*>> castAndRender(data: RenderEventData, result: GlFramebuffer, temp: GlFramebuffer, type: BlockLightType<*, S>, storage: BlockLightStorage<*>, profiler: ProfilerFiller) {
        profiler.push(BlockLightRegistry.registry!!.getKey(type).location.toString())
        type.render(this, result, temp, data, storage as S, getDebugOutput(BlockLightRegistry.registry!!.getKey(type)), profiler)
        profiler.pop()
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <S : SkyLightStorage<*>> castAndRender(data: RenderEventData, result: GlFramebuffer, temp: GlFramebuffer, type: SkyLightType<*, S>, storage: SkyLightStorage<*>, profiler: ProfilerFiller) {
        profiler.push(SkyLightRegistry.registry!!.getKey(type).location.toString())
        type.render(this, result, temp, data, storage as S, getDebugOutput(SkyLightRegistry.registry!!.getKey(type)), profiler)
        profiler.pop()
    }

    fun isSectionVisible(pos: SectionPos): Boolean {
        val renderer = (Minecraft.getInstance().levelRenderer as LevelRendererExtension).`sodium$getWorldRenderer`()
        val sectionManager = (renderer as SodiumWorldRendererAccessor).`vibrancy$getRenderSectionManager`()
        return sectionManager.isSectionVisible(pos.x, pos.y, pos.z)
    }

    //? if <1.21.5 {
    /*fun render(data: RenderEventData, result: GlFramebuffer, temp: GlFramebuffer, profiler: ProfilerFiller = Minecraft.getInstance().profiler) {
    *///? } else {
    fun render(data: RenderEventData, result: GlFramebuffer, temp: GlFramebuffer, profiler: ProfilerFiller = Profiler.get()) {
    //? }
        profiler.push("vibrancy")
        debugInfo.clear()

        synchronized(sectionLock) {
            dirtySections = nextDirtySections
            nextDirtySections = LinkedList()
        }

        for (entry in blockLights) {
            castAndRender(data, result, temp, entry.key, entry.value, profiler)
        }

        skyLight?.let { castAndRender(data, result, temp, it.first, it.second, profiler) }

        dirtySections.clear()
        dirtyBlocks.clear()
        profiler.pop()
    }
     */

    fun getDebugOutput(out: Consumer<String>) {
        debugInfo[null]?.forEach { (key, value) -> out.accept("$key: $value") }

        for (entry in blockLights) {
            val key = BlockLightRegistry.registry.getKey(entry.key)!!
            out.accept(ChatFormatting.UNDERLINE.toString() + key.toString())
            out.accept("lightsInWorld: ${entry.value.size}")

            debugInfo[key]?.forEach { (key, value) -> out.accept("$key: $value") }
        }

        skyLight?.let {
            val key = SkyLightRegistry.registry.getKey(it.first)!!
            out.accept(ChatFormatting.UNDERLINE.toString() + "Sky Light: " + key.toString())
            debugInfo[key]?.forEach { (key, value) -> out.accept("$key: $value") }
        }
    }

    fun getRenderDistance(chunks: Int): Int {
        val d = chunks.coerceAtMost(Minecraft.getInstance().options.effectiveRenderDistance)
        return d * d * 256
    }

    fun getGridRenderDistance(chunks: Int): Int {
        return chunks.coerceAtMost(Minecraft.getInstance().options.effectiveRenderDistance) * 16
    }
}