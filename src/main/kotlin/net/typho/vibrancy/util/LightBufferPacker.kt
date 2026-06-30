package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion
import net.minecraft.core.SectionPos
import net.minecraft.world.entity.EntityType.by
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.RenderRegionExtension
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLight

object LightBufferPacker {
    @JvmStatic
    fun pack(region: RenderRegion, lights: Iterable<RayPointLight>, manager: LightManager, output: RenderRegionExtension) {
        var numShadows = 0
        val shadows = mutableListOf<Pair<List<BlockFace>, Int>>()
        val lightGrid = Array(256) { mutableListOf<Pair<RayPointLight, Int>>() }
        val sectionMeshes = hashMapOf<SectionPos, SectionMeshCache?>()
        var numLightInstances = 0
        var numLights = 0

        for (light in lights) {
            var added = false
            val faces by lazy {
                val faces = mutableListOf<BlockFace>() // TODO grid

                light.shadowBox.iterator().forEach { pos ->
                    if (pos != light.pos) {
                        val sectionPos = SectionPos.of(
                            SectionPos.blockToSectionCoord(pos.x),
                            SectionPos.blockToSectionCoord(pos.y),
                            SectionPos.blockToSectionCoord(pos.z)
                        )
                        sectionMeshes.computeIfAbsent(sectionPos) { key -> synchronized(manager.sectionLock) { manager.sectionMeshCaches[key] } }?.let { section ->
                            section.get(pos.x, pos.y, pos.z)?.let { block ->
                                block.collect { faces.add(it.copyWithOffset(pos.x, pos.y, pos.z)) }
                            }
                        }
                    }
                }

                val index = shadows.size
                shadows.add(faces to numShadows)
                numShadows += faces.size
                index
            }

            for (pos in light.sections) {
                if ((pos.x shr 3) == region.x && (pos.y shr 2) == region.y && (pos.z shr 3) == region.z) {
                    lightGrid[LocalSectionIndex.pack(pos.x, pos.y, pos.z)].add(light to faces)
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

        if (numShadows == 0) {
            Vibrancy.LOGGER.warn("No shadows, yet $numLights lights? Skipping")
            output.`vibrancy$clear`()
            return
        }

        val lightBuffer = GpuObjects.buffer(
            { "Vibrancy Light Buffer (${region.x}, ${region.y}, ${region.z})" },
            16L + 1024L + numLightInstances * 32L,
            GpuBufferUsage.UNIFORM or GpuBufferUsage.COPY_DST // TODO optimize usage
        )

        lightBuffer.upload { output ->
            output.writeInt(region.originX)
            output.writeInt(region.originY)
            output.writeInt(region.originZ)

            var index = 0

            for (cell in lightGrid) {
                val index1 = index
                index += cell.size
                output.write2x2(index1, index)
            }

            output.skip(4)

            for (cell in lightGrid) {
                for (light in cell) {
                    output.writeFloat(light.first.absolutePos.x)
                    output.writeFloat(light.first.absolutePos.y)
                    output.writeFloat(light.first.absolutePos.z)

                    output.write4x1(
                        (light.first.radius / 16 * 255).toInt(),
                        (light.first.color.z * 255).toInt(),
                        (light.first.color.y * 255).toInt(),
                        (light.first.color.x * 255).toInt()
                    )

                    val shadows = shadows[light.second]
                    output.writeInt(shadows.second)
                    output.writeInt(shadows.first.size + shadows.second)

                    output.skip(8)
                }
            }
        }

        val shadowBuffer = GpuObjects.buffer(
            { "Vibrancy Shadow Buffer (${region.x}, ${region.y}, ${region.z})" },
            numShadows * 32L * 4L,
            GpuBufferUsage.UNIFORM or GpuBufferUsage.COPY_DST // TODO optimize usage
        )

        shadowBuffer.upload { output ->
            for (faces in shadows) {
                for (face in faces.first) {
                    face.apply { vertex ->
                        output.writeFloat(vertex.x)
                        output.writeFloat(vertex.y)
                        output.writeFloat(vertex.z)
                        output.writeInt(vertex.color)

                        output.writeFloat(vertex.u)
                        output.writeFloat(vertex.v)

                        output.skip(8)
                    }
                }
            }
        }

        output.`vibrancy$lightBuffer` = lightBuffer
        output.`vibrancy$shadowBuffer` = shadowBuffer
    }
}