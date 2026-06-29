package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLight

object LightBufferPacker {
    @JvmStatic
    fun pack(region: RenderRegion, lights: Iterable<RayPointLight>): GpuBuffer? {
        val grid = Array(256) { mutableListOf<RayPointLight>() }
        var numLightInstances = 0
        var numLights = 0

        for (light in lights) {
            var added = false

            for (pos in light.sections) {
                if ((pos.x shr 3) == region.x && (pos.y shr 2) == region.y && (pos.z shr 3) == region.z) {
                    grid[LocalSectionIndex.pack(pos.x, pos.y, pos.z)].add(light)
                    numLightInstances++

                    if (!added) {
                        added = true
                        numLights++
                    }
                }
            }
        }

        if (numLightInstances == 0) {
            return null
        }

        if (numLightInstances > Short.MAX_VALUE) {
            Vibrancy.LOGGER.warn("Unreasonably high amount of lights in a region ($numLightInstances instances, $numLights lights), skipping.")
            return null
        }

        val buffer = GpuObjects.buffer(
            { "Vibrancy Light Buffer (${region.x}, ${region.y}, ${region.z})" },
            16L + 1024L + numLightInstances * 16L,
            GpuBufferUsage.UNIFORM or GpuBufferUsage.COPY_DST
        )

        buffer.upload { output ->
            output.writeInt(region.originX)
            output.writeInt(region.originY)
            output.writeInt(region.originZ)

            var index = 0

            for (cell in grid) {
                val index1 = index
                index += cell.size
                output.write2x2(index1, index)
            }

            output.skip(4)

            for (cell in grid) {
                for (light in cell) {
                    output.writeFloat(light.absolutePos.x)
                    output.writeFloat(light.absolutePos.y)
                    output.writeFloat(light.absolutePos.z)

                    output.write4x1(
                        (light.radius / 16 * 255).toInt(),
                        (light.color.z * 255).toInt(),
                        (light.color.y * 255).toInt(),
                        (light.color.x * 255).toInt()
                    )
                }
            }
        }

        return buffer
    }
}