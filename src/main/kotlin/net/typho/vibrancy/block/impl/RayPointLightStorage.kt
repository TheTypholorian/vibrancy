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
import kotlin.math.abs

class RayPointLightStorage : HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>(RayPointLightType) {
    class RegionData(
        @JvmField
        val lightBuffer: GpuBuffer,
        @JvmField
        val shadowBuffer: GpuBuffer?,
        @JvmField
        val cheeseBuffer: GpuBuffer
    ) : Recyclable {
        override fun recycle() {
            lightBuffer.recycle()
            shadowBuffer?.recycle()
            cheeseBuffer.recycle()
        }
    }

    companion object {
        const val NUM_CHEESE_WEDGES = 24

        @JvmStatic
        fun getWedgeIndex(lightPos: IVec3<Float>, fragPos: IVec3<Float>): Int {
            val delta = (lightPos - fragPos).abs
            var index = 0

            if (fragPos.x >= lightPos.x) {
                index = index or 1
            }

            if (fragPos.y >= lightPos.y) {
                index = index or 2
            }

            if (fragPos.z >= lightPos.z) {
                index = index or 4
            }

            index *= 3

            index += if (delta.x >= delta.y && delta.x >= delta.z) {
                0
            } else if (delta.y >= delta.z) {
                1
            } else {
                2
            }

            /*
            index += if (delta.x >= delta.y) {
                if (delta.y >= delta.z) {
                    0
                } else if (delta.x >= delta.z) {
                    1
                } else {
                    2
                }
            } else {
                if (delta.y >= delta.z) {
                    3
                } else if (delta.x >= delta.z) {
                    4
                } else {
                    5
                }
            }
             */

            return index
        }

        @JvmStatic
        fun getWedgeIndex(lightPos: IVec3<Float>, x: Int, y: Int, z: Int): Int {
            val deltaX = abs(lightPos.x - x)
            val deltaY = abs(lightPos.y - y)
            val deltaZ = abs(lightPos.z - z)
            var index = 0

            if (x >= lightPos.x) {
                index = index or 1
            }

            if (y >= lightPos.y) {
                index = index or 2
            }

            if (z >= lightPos.z) {
                index = index or 4
            }

            index *= 3

            index += if (deltaX >= deltaY && deltaX >= deltaZ) {
                0
            } else if (deltaY >= deltaZ) {
                1
            } else {
                2
            }

            /*
            index += if (delta.x >= delta.y) {
                if (delta.y >= delta.z) {
                    0
                } else if (delta.x >= delta.z) {
                    1
                } else {
                    2
                }
            } else {
                if (delta.y >= delta.z) {
                    3
                } else if (delta.x >= delta.z) {
                    4
                } else {
                    5
                }
            }
             */

            return index
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
                    val cheese: IntArray
                )

                var cheeseIndex = 0
                val shadows = mutableListOf<BlockFace>()
                var hasShadows = false
                val blockShadows = mutableMapOf<IVec3<Int>, Int?>()
                val sectionGrid = Array(256) { mutableListOf<LightInstance>() }
                val sectionMeshes = hashMapOf<SectionPos, SectionMeshCache?>()
                val cheese = mutableListOf<List<Int?>>()
                var numLightInstances = 0
                var numLights = 0

                fun getSectionMesh(sectionPos: SectionPos) = sectionMeshes.computeIfAbsent(sectionPos) { key -> synchronized(manager.sectionLock) { manager.sectionMeshCaches[key] } }

                fun getGridIndex(voxel: IVec3<Int>, box: IRect3<Int>): Int {
                    return ((voxel.x - box.min.x) * (box.max.y - box.min.y + 1) + (voxel.y - box.min.y)) * (box.max.z - box.min.z + 1) + (voxel.z - box.min.z)
                }

                fun getBlockShadow(section: SectionMeshCache, pos: BlockPos): Int? {
                    return blockShadows.computeIfAbsent(pos.immutable()) {
                        section.get(pos.x, pos.y, pos.z)?.let { block ->
                            val start = shadows.size
                            block.collect { shadows.add(it.copyWithOffset(pos.x, pos.y, pos.z)) }
                            val end = shadows.size
                            val len = end - start

                            if (start and 524287.inv() != 0) {
                                throw IndexOutOfBoundsException(start)
                            }

                            if (len and 4095.inv() != 0) {
                                throw IndexOutOfBoundsException(len)
                            }

                            (start shl 13) or (len shl 1)
                        }
                    }
                }

                for (light in lights) {
                    if (isCancelled()) {
                        return@submitClean null
                    }

                    var added = false
                    val wedges by lazy {
                        val wedges = Array<MutableList<Int>>(NUM_CHEESE_WEDGES) { mutableListOf() }
                        //val grid = arrayOfNulls<Int>(light.shadowBox.areaInclusive)

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
                                    val cell = if (pos == light.pos) {
                                        getBlockShadow(mesh, pos)
                                    } else {
                                        val index = mesh.index(pos.x, pos.y, pos.z) shl 1

                                        if (mesh.stateFlags.get(index)) { // air
                                            null
                                        } else if (mesh.stateFlags.get(index + 1)) { // solid
                                            1
                                        } else {
                                            getBlockShadow(mesh, pos)
                                        }
                                    }
                                    cell?.let {
                                        val arr = BooleanArray(NUM_CHEESE_WEDGES)

                                        fun test(x: Int, y: Int, z: Int) {
                                            val index = getWedgeIndex(light.absolutePos, x, y, z)

                                            if (!arr[index]) {
                                                arr[index] = true
                                                wedges[index].add(it)
                                            }
                                        }

                                        test(pos.x    , pos.y    , pos.z    )
                                        test(pos.x    , pos.y    , pos.z + 1)
                                        test(pos.x    , pos.y + 1, pos.z    )
                                        test(pos.x    , pos.y + 1, pos.z + 1)
                                        test(pos.x + 1, pos.y    , pos.z    )
                                        test(pos.x + 1, pos.y    , pos.z + 1)
                                        test(pos.x + 1, pos.y + 1, pos.z    )
                                        test(pos.x + 1, pos.y + 1, pos.z + 1)

                                        hasShadows = true
                                    }
                                }
                            }
                        }

                        val wedgeRanges = IntArray(NUM_CHEESE_WEDGES)

                        wedges.forEachIndexed { index, wedge ->
                            val cheeseStart = cheeseIndex
                            cheese.add(wedge)
                            cheeseIndex += wedge.size
                            val cheeseLength = cheeseIndex - cheeseStart
                            wedgeRanges[index] = (cheeseStart shl 18) or cheeseLength
                        }

                        wedgeRanges
                    }

                    for (pos in light.sections) {
                        if ((pos.x shr 3) == region.x && (pos.y shr 2) == region.y && (pos.z shr 3) == region.z) {
                            sectionGrid[LocalSectionIndex.pack(pos.x, pos.y, pos.z)].add(LightInstance(light, wedges))
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
                        16L + 1024L + numLightInstances * 128L,
                        bufferUsage
                    ) { output ->
                        output.writeInt(region.originX)
                        output.writeInt(region.originY)
                        output.writeInt(region.originZ)

                        var index = 0

                        for (cell in sectionGrid) {
                            val index1 = index
                            index += cell.size
                            output.write2x2(index1, index)
                        }

                        output.skip(4)

                        for (cell in sectionGrid) {
                            for (light in cell) {
                                output.writeFloat(light.light.absolutePos.x)
                                output.writeFloat(light.light.absolutePos.y)
                                output.writeFloat(light.light.absolutePos.z)

                                output.write4x1(
                                    0,
                                    (light.light.color.z * 255).toInt(),
                                    (light.light.color.y * 255).toInt(),
                                    (light.light.color.x * 255).toInt()
                                )

                                output.writeFloat(light.light.radius)
                                output.writeInt(light.light.shadowRadius)
                                //output.writeInt(light.cellRangeStart)

                                output.writeFloat(light.light.brightness)

                                for (i in light.cheese) {
                                    output.writeInt(i)
                                }

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

                    val cheeseBuffer = GpuObjects.buffer(
                        { "Vibrancy Cheese Buffer $regionPos" },
                        cheeseIndex * 4L,
                        bufferUsage
                    ) { output ->
                        for (grid in cheese) {
                            for (cell in grid) {
                                output.writeInt(cell ?: 0) // must write 0, cannot skip bytes here
                            }
                        }
                    }

                    Vibrancy.LOGGER.info("Uploading $regionPos: ${lightBuffer.size} light buffer, ${shadowBuffer?.size} shadow buffer, ${cheeseBuffer.size} grid buffer, total of ${lightBuffer.size + (shadowBuffer?.size ?: 0) + cheeseBuffer.size} bytes. $numLights lights, $numLightInstances light instances, ${shadows.size} shadows, meaning ${shadows.size / numLights} shadows per light, ${shadows.size / numLightInstances} shadows per light instance, max grid cell index $cheeseIndex, num non-solid blocks ${blockShadows.size}, num sections ${sectionMeshes.size}")

                    RegionData(lightBuffer, shadowBuffer, cheeseBuffer)
                }
            })
            oldTask?.cancel()
        }

        return oldRegionData
    }
}