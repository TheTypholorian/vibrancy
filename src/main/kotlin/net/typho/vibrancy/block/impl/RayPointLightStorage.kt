package net.typho.vibrancy.block.impl

import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.Recyclable
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.big_shot_lib.api.math.IRect3
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.util.BlockFace
import net.typho.vibrancy.util.GpuTask
import net.typho.vibrancy.util.SectionMeshCache
import net.typho.vibrancy.util.VibrancyThreadPool
import kotlin.experimental.or

class RayPointLightStorage : HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>(RayPointLightType) {
    class RegionData(
        @JvmField
        val lightBuffer: GpuBuffer,
        @JvmField
        val shadowBuffer: GpuBuffer?,
        @JvmField
        val gridBuffer: GpuBuffer
    ) : Recyclable {
        override fun recycle() {
            lightBuffer.recycle()
            shadowBuffer?.recycle()
            gridBuffer.recycle()
        }
    }

    @JvmField
    var dirty = false
    @JvmField
    val regions = mutableMapOf<IVec3<Int>, RegionData?>()
    @JvmField
    val tasks = mutableMapOf<IVec3<Int>, GpuTask<(() -> RegionData)?>>()

    override fun shouldCollectMeshGeometry(pos: SectionPos): Boolean {
        synchronized(map) {
            return map.values.any { it.sections.contains(pos) }
        }
    }

    override fun addLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: RayPointLightInfo
    ) {
        super.addLight(manager, level, state, pos, info)
        dirty = true
    }

    override fun removeLight(manager: LightManager, level: Level, pos: IVec3<Int>): Boolean {
        val r = super.removeLight(manager, level, pos)
        dirty = dirty or r
        return r
    }

    override fun loadChunk(manager: LightManager, chunk: ChunkAccess) {
        super.loadChunk(manager, chunk)
        dirty = true
    }

    override fun deloadChunk(manager: LightManager, chunk: ChunkAccess) {
        super.deloadChunk(manager, chunk)
        dirty = true
    }

    override fun clear(manager: LightManager) {
        super.clear(manager)
        regions.values.forEach { it?.recycle() }
        regions.clear()
        dirty = true
    }

    override fun createLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: RayPointLightInfo
    ) = if (info.enabled(state)) RayPointLight(level, info, state, pos) else null

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        synchronized(map) {
            if (chunk == null) {
                map.values.forEach { it.reload() }
            } else {
                map.values.filter { ChunkPos.containing(it.pos.toBlockPos()) == chunk }
                    .forEach { it.reload() }
            }
        }
    }

    override fun endFrame(manager: LightManager) {
        dirty = false
    }

    fun getOrPackRegion(region: RenderRegion, manager: LightManager): RegionData? {
        tasks.entries.removeIf { (regionPos, task) ->
            if (task.isDoneOrCancelled()) {
                val result = task.finish()?.invoke()
                val old = regions.put(regionPos, result)
                old?.recycle()

                true
            } else {
                false
            }
        }

        val regionPos = IVec3(region.x, region.y, region.z)
        val oldRegionData = regions[regionPos]

        if (dirty || manager.isRenderRegionOrNeighborsDirty(region) || (!regions.keys.contains(regionPos) && !tasks.keys.contains(regionPos))) {
            val lights = map.values.toList()
            val oldTask = tasks.put(regionPos, VibrancyThreadPool.submitClean(0.0) { isCancelled ->
                class LightInstance(
                    @JvmField
                    val light: RayPointLight,
                    @JvmField
                    val shadowRangeStart: Int,
                    @JvmField
                    val shadowRangeLightLength: Int,
                    @JvmField
                    val shadowRangeLength: Int,
                    @JvmField
                    val cellRangeStart: Int
                )

                var cellRangeIndex = 0
                val shadows = mutableListOf<BlockFace>()
                var hasShadows = false
                val blockShadows = mutableMapOf<IVec3<Int>, Int>()
                val sectionGrid = Array(256) { mutableListOf<LightInstance>() }
                val sectionMeshes = hashMapOf<SectionPos, SectionMeshCache?>()
                val grids = mutableListOf<ByteArray>()
                var numLightInstances = 0
                var numLights = 0

                fun getSectionMesh(sectionPos: SectionPos) = sectionMeshes.computeIfAbsent(sectionPos) { key -> synchronized(manager.sectionLock) { manager.sectionMeshCaches[key] } }

                fun getBlockShadow(section: SectionMeshCache, pos: BlockPos): Int {
                    return blockShadows.computeIfAbsent(pos.immutable()) {
                        section.get(pos.x, pos.y, pos.z)?.let { block ->
                            val start = shadows.size
                            block.collect { shadows.add(it.copyWithOffset(pos.x, pos.y, pos.z)) }
                            val end = shadows.size
                            val len = end - start

                            if (start and 524287.inv() != 0) {
                                throw IndexOutOfBoundsException(start)
                            }

                            if (len and 8191.inv() != 0) {
                                throw IndexOutOfBoundsException(len)
                            }

                            (start shl 13) or len
                        } ?: 0
                    }
                }

                for (light in lights) {
                    if (isCancelled()) {
                        return@submitClean null
                    }

                    var added = false
                    val lightInstance by lazy {
                        val grid = ByteArray(light.shadowBox.areaInclusive)

                        fun setShadowGridBit(voxel: IVec3<Int>) {
                            val index = ((voxel.x - light.shadowBox.min.x) * (light.shadowBox.max.y - light.shadowBox.min.y + 1) + (voxel.y - light.shadowBox.min.y)) * (light.shadowBox.max.z - light.shadowBox.min.z + 1) + (voxel.z - light.shadowBox.min.z)
                            grid[index] = 1
                        }

                        SectionPos.betweenClosedStream(
                            SectionPos.blockToSectionCoord(light.shadowBox.min.x),
                            SectionPos.blockToSectionCoord(light.shadowBox.min.y),
                            SectionPos.blockToSectionCoord(light.shadowBox.min.z),
                            SectionPos.blockToSectionCoord(light.shadowBox.max.x),
                            SectionPos.blockToSectionCoord(light.shadowBox.max.y),
                            SectionPos.blockToSectionCoord(light.shadowBox.max.z)
                        ).forEach { sectionPos ->
                            getSectionMesh(sectionPos)?.let { mesh ->
                                BlockBox(
                                    BlockPos(
                                        sectionPos.minBlockX(),
                                        sectionPos.minBlockY(),
                                        sectionPos.minBlockZ()
                                    ).max(light.shadowBox.min),
                                    BlockPos(
                                        sectionPos.maxBlockX(),
                                        sectionPos.maxBlockY(),
                                        sectionPos.maxBlockZ()
                                    ).min(light.shadowBox.max)
                                ).forEach { pos ->
                                    if (pos != light.pos) {
                                        val index = mesh.index(pos.x, pos.y, pos.z) shl 1

                                        if (mesh.stateFlags.get(index + 1)) {
                                            setShadowGridBit(pos)
                                            hasShadows = true
                                        }
                                    }
                                }
                            }
                        }

                        val cellRangeStart = cellRangeIndex

                        grids.add(grid)
                        cellRangeIndex += grid.size

                        LightInstance(light, 0, 0, 0, cellRangeStart)
                    }

                    for (pos in light.sections) {
                        if ((pos.x shr 3) == region.x && (pos.y shr 2) == region.y && (pos.z shr 3) == region.z) {
                            sectionGrid[LocalSectionIndex.pack(pos.x, pos.y, pos.z)].add(lightInstance)
                            numLightInstances++

                            if (!added) {
                                added = true
                                numLights++
                            }
                        }
                    }
                }

                if (numLightInstances == 0) {
                    regions[regionPos] = null
                    return@submitClean null
                }

                if (numLightInstances > Short.MAX_VALUE) {
                    Vibrancy.LOGGER.warn("Unreasonably high amount of lights in a region ($numLightInstances instances, $numLights lights), skipping.")
                    regions[regionPos] = null
                    return@submitClean null
                }

                if (!hasShadows) {
                    Vibrancy.LOGGER.warn("No shadows, yet $numLights lights? Skipping ${region.x} ${region.y} ${region.z}")
                    regions[regionPos] = null
                    return@submitClean null
                }

                {
                    val bufferUsage = GpuBufferUsage.SHADER_STORAGE

                    val lightBuffer = GpuObjects.buffer(
                        { "Vibrancy Light Buffer $regionPos" },
                        16L + 1024L + numLightInstances * 48L,
                        bufferUsage
                    ) { output ->
                        output.writeInt(region.originX)
                        output.writeInt(region.originY)
                        output.writeInt(region.originZ)

                        var index = 0

                        for (cell in sectionGrid) {
                            output.write2x2(index, cell.size)
                            index += cell.size
                        }

                        output.skip(4)

                        for (cell in sectionGrid) {
                            for (light in cell) {
                                output.writeFloat(light.light.absolutePos.x)
                                output.writeFloat(light.light.absolutePos.y)
                                output.writeFloat(light.light.absolutePos.z)
                                output.writeFloat(light.light.radius)

                                output.write4x1(
                                    0,
                                    (light.light.color.z * 255).toInt(),
                                    (light.light.color.y * 255).toInt(),
                                    (light.light.color.x * 255).toInt()
                                )
                                output.writeFloat(light.light.brightness)

                                output.writeInt(light.light.shadowRadius)
                                output.writeInt(light.shadowRangeStart)
                                output.writeInt(light.shadowRangeLightLength)
                                output.writeInt(light.shadowRangeLength)
                                output.writeInt(light.cellRangeStart)

                                output.skip(4)
                            }
                        }
                    }

                    val shadowBuffer = if (shadows.isEmpty()) null else GpuObjects.buffer(
                        { "Vibrancy Shadow Buffer $regionPos" },
                        shadows.size * 32L * 4L,
                        bufferUsage
                    ) { output ->
                        for (face in shadows) {
                            face.apply { vertex ->
                                output.writeFloat(vertex.x)
                                output.writeFloat(vertex.y)
                                output.writeFloat(vertex.z)

                                output.writeInt(vertex.color)
                                output.write2x2(
                                    (vertex.v * 65535).toInt(),
                                    (vertex.u * 65535).toInt()
                                )

                                output.skip(12)
                            }
                        }
                    }

                    val gridBuffer = GpuObjects.buffer(
                        { "Vibrancy Shadow Grid Buffer $regionPos" },
                        cellRangeIndex.toLong(),
                        bufferUsage
                    ) { output ->
                        for (grid in grids) {
                            for (cell in grid) {
                                output.writeByte(cell.toInt())
                            }
                        }
                    }

                    Vibrancy.LOGGER.info("Uploading $regionPos: ${lightBuffer.size} light buffer, ${shadowBuffer?.size} shadow buffer, ${gridBuffer.size} grid buffer, total of ${lightBuffer.size + (shadowBuffer?.size ?: 0) + gridBuffer.size} bytes. $numLights lights, $numLightInstances light instances, ${shadows.size} shadows, meaning ${shadows.size / numLights} shadows per light, ${shadows.size / numLightInstances} shadows per light instance, max grid cell index $cellRangeIndex, num non-solid blocks ${blockShadows.size}, num sections ${sectionMeshes.size}, ${sectionGrid.sumOf { it.size }.toFloat() / sectionGrid.size} section grid, ${blockShadows.values.sumOf { it and 8191 }.toFloat() / blockShadows.values.sumOf { if (it == 0) 0 else 1 }} quads per non-solid non-air block")

                    RegionData(lightBuffer, shadowBuffer, gridBuffer)
                }
            })
            oldTask?.cancel()
        }

        return oldRegionData
    }
}