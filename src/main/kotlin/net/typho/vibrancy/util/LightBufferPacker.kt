package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.levelgen.SurfaceRules.state
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.big_shot_lib.api.math.IRect3
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.RenderRegionExtension
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLight

object LightBufferPacker {
    const val SHADOW_GRID_SHIFT = 1
    const val SHADOW_GRID_SIZE = 1 shl SHADOW_GRID_SHIFT
    const val SHADOW_GRID_AREA = SHADOW_GRID_SIZE * SHADOW_GRID_SIZE * SHADOW_GRID_SIZE

    @JvmStatic
    fun pack(region: RenderRegion, lights: Iterable<RayPointLight>, manager: LightManager, output: RenderRegionExtension) {
        class SectionData(
            @JvmField
            val light: RayPointLight,
            @JvmField
            val cellRangeStart: Int
        )

        class ShadowGridCell(
            @JvmField
            val voxels: Array<Int?> = arrayOfNulls(SHADOW_GRID_AREA)
        ) {
            fun getIndex(x: Int, y: Int, z: Int): Int {
                return (((x shl SHADOW_GRID_SHIFT) + y) shl SHADOW_GRID_SHIFT) + z
            }

            operator fun get(x: Int, y: Int, z: Int): Int? {
                return voxels[getIndex(x, y, z)]
            }
        }

        var cellRangeIndex = 0
        val shadows = mutableListOf<BlockFace>()
        var hasShadows = false
        val blockShadows = mutableMapOf<IVec3<Int>, Int?>()
        val sectionGrid = Array(256) { mutableListOf<SectionData>() }
        val sectionCache = ChunkSectionCache(Minecraft.getInstance().level!!)
        val sectionMeshes = hashMapOf<SectionPos, SectionMeshCache?>()
        val grids = mutableListOf<Array<ShadowGridCell?>>()
        var numLightInstances = 0
        var numLights = 0

        fun getGridIndex(voxel: IVec3<Int>, box: IRect3<Int>): Int {
            val voxel = voxel - box.min
            return (voxel.x * box.size.x + voxel.y) * box.size.y + voxel.z
        }

        fun getBlockShadow(pos: BlockPos): Int? {
            return blockShadows.computeIfAbsent(pos.immutable()) {
                val sectionPos = SectionPos.of(
                    SectionPos.blockToSectionCoord(pos.x),
                    SectionPos.blockToSectionCoord(pos.y),
                    SectionPos.blockToSectionCoord(pos.z)
                )
                sectionMeshes.computeIfAbsent(sectionPos) { key -> synchronized(manager.sectionLock) { manager.sectionMeshCaches[key] } }?.let { section ->
                    section.get(pos.x, pos.y, pos.z)?.let { block ->
                        val start = shadows.size
                        block.collect { shadows.add(it.copyWithOffset(pos.x, pos.y, pos.z)) }
                        val end = shadows.size
                        val len = end - start

                        if (start != end) {
                            hasShadows = true
                        }

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
        }

        for (light in lights) {
            var added = false
            val cellRangeStart by lazy {
                val shadowBox = light.getShadowBox(SHADOW_GRID_SHIFT)
                val grid = arrayOfNulls<ShadowGridCell>(shadowBox.area)

                for (pos in shadowBox.copyWithUnchecked(shadowBox.min, shadowBox.max - 1).iterator()) {
                    val worldPos = light.shadowGridCellPosToWorldPos(pos, SHADOW_GRID_SHIFT)
                    val cell = ShadowGridCell()
                    var relativeIndex = 0

                    for (x in 0 until SHADOW_GRID_SIZE) {
                        for (y in 0 until SHADOW_GRID_SIZE) {
                            for (z in 0 until SHADOW_GRID_SIZE) {
                                val pos = worldPos.plus(x, y, z).toBlockPos()
                                val state = sectionCache[pos] // TODO optimize?

                                cell.voxels[relativeIndex++] = if (pos == light.pos) {
                                    getBlockShadow(pos)
                                } else {
                                    if (state.isAir) {
                                        null
                                    } else if (state.isSolidRender) {
                                        hasShadows = true
                                        1
                                    } else {
                                        getBlockShadow(pos.toBlockPos())
                                    }
                                }
                            }
                        }
                    }

                    grid[getGridIndex(pos, shadowBox)] = cell
                }

                val start = cellRangeIndex

                grids.add(grid) // TODO why could be null?
                cellRangeIndex += grid.size

                start
            }

            for (pos in light.sections) {
                if ((pos.x shr 3) == region.x && (pos.y shr 2) == region.y && (pos.z shr 3) == region.z) {
                    sectionGrid[LocalSectionIndex.pack(pos.x, pos.y, pos.z)].add(SectionData(light, cellRangeStart))
                    numLightInstances++

                    if (!added) {
                        added = true
                        numLights++
                    }
                }
            }
        }

        if (numLightInstances == 0) {
            output.`vibrancy$clear`()
            return
        }

        if (numLightInstances > Short.MAX_VALUE) {
            Vibrancy.LOGGER.warn("Unreasonably high amount of lights in a region ($numLightInstances instances, $numLights lights), skipping.")
            output.`vibrancy$clear`()
            return
        }

        if (!hasShadows) {
            Vibrancy.LOGGER.warn("No shadows, yet $numLights lights? Skipping ${region.x} ${region.y} ${region.z}")
            output.`vibrancy$clear`()
            return
        }

        val bufferUsage = GpuBufferUsage.UNIFORM

        val lightBuffer = GpuObjects.buffer(
            { "Vibrancy Light Buffer (${region.x}, ${region.y}, ${region.z})" },
            16L + 1024L + numLightInstances * 32L,
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
                    output.writeInt(light.light.shadowRadius ushr SHADOW_GRID_SHIFT)
                    output.writeInt(light.cellRangeStart)

                    output.skip(4)
                }
            }
        }

        val shadowBuffer = if (shadows.isEmpty()) null else GpuObjects.buffer(
            { "Vibrancy Shadow Buffer (${region.x}, ${region.y}, ${region.z})" },
            shadows.size * 32L * 4L,
            bufferUsage
        ) { output ->
            for (face in shadows) {
                face.apply { vertex ->
                    output.writeFloat(vertex.x)
                    output.writeFloat(vertex.y)
                    output.writeFloat(vertex.z)
                    //output.writeShort(((vertex.x - 0.5f) / 1.5f * 32767).toInt())
                    //output.writeShort(((vertex.y - 0.5f) / 1.5f * 32767).toInt())
                    //output.writeShort(((vertex.z - 0.5f) / 1.5f * 32767).toInt())
                    //output.skip(2)

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
            { "Vibrancy Shadow Grid Buffer (${region.x}, ${region.y}, ${region.z})" },
            cellRangeIndex * SHADOW_GRID_AREA * 4L,
            bufferUsage
        ) { output ->
            for (grid in grids) {
                for (cell in grid) {
                    if (cell == null) {
                        repeat(SHADOW_GRID_AREA) {
                            output.writeInt(0)
                        }
                    } else {
                        for (voxel in cell.voxels) {
                            output.writeInt(voxel ?: 0) // must write 0, cannot skip bytes here
                        }
                    }
                }
            }
        }

        Vibrancy.LOGGER.info("Uploading ${lightBuffer.size} light buffer, ${shadowBuffer?.size} shadow buffer, ${gridBuffer.size} grid buffer, total of ${lightBuffer.size + (shadowBuffer?.size ?: 0) + gridBuffer.size} bytes. $numLights lights, $numLightInstances light instances, ${shadows.size} shadows, meaning ${shadows.size / numLights} shadows per light, ${shadows.size / numLightInstances} shadows per light instance, max grid cell index $cellRangeIndex")

        output.`vibrancy$lightBuffer` = lightBuffer
        output.`vibrancy$shadowBuffer` = shadowBuffer
        output.`vibrancy$gridBuffer` = gridBuffer
    }
}