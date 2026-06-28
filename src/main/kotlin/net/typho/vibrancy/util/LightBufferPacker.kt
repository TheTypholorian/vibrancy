package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion
import net.minecraft.util.Mth
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.vibrancy.block.impl.RayPointLight

object LightBufferPacker {
    @JvmStatic
    fun pack(region: RenderRegion, lights: Iterable<RayPointLight>): GpuBuffer? {
        val grid = Array(256) { mutableListOf<RayPointLight>() }
        var numLights = 0

        for (light in lights) {
            val section = light.sectionPos
            val relX = section.x - region.chunkX
            val relY = section.y - region.chunkY
            val relZ = section.z - region.chunkZ

            if (relX < -1 || relX > RenderRegion.REGION_WIDTH) {
                continue
            }

            if (relY < -1 || relY > RenderRegion.REGION_HEIGHT) {
                continue
            }

            if (relZ < -1 || relZ > RenderRegion.REGION_LENGTH) {
                continue
            }

            grid[LocalSectionIndex.pack(
                Mth.clamp(relX, 0, RenderRegion.REGION_WIDTH - 1),
                Mth.clamp(relY, 0, RenderRegion.REGION_HEIGHT - 1),
                Mth.clamp(relZ, 0, RenderRegion.REGION_LENGTH - 1)
            )].add(light)
            numLights++
        }

        if (numLights == 0) {
            return null
        }

        val buffer = GpuObjects.buffer(
            { "Vibrancy Light Buffer (${region.x}, ${region.y}, ${region.z})" },
            16L + 1024L + numLights * 32L,
            GpuBufferUsage.UNIFORM or GpuBufferUsage.COPY_DST
        )

        buffer.upload { output ->
            output.writeInt(region.originX)
            output.writeInt(region.originY)
            output.writeInt(region.originZ)
            output.skip(4)

            var index = 0

            for (cell in grid) {
                val index1 = index
                index += cell.size
                output.write2x2(index1, index)
            }

            for (cell in grid) {
                for (light in cell) {
                    output.writeFloat(light.absolutePos.x)
                    output.writeFloat(light.absolutePos.y)
                    output.writeFloat(light.absolutePos.z)

                    output.writeFloat(light.radius)

                    output.writeFloat(light.color.x)
                    output.writeFloat(light.color.y)
                    output.writeFloat(light.color.z)

                    output.skip(4)
                }
            }
        }

        return buffer
    }
}