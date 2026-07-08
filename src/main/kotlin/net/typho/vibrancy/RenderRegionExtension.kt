package net.typho.vibrancy

import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage

interface RenderRegionExtension {
    var `vibrancy$initialized`: Boolean
    var `vibrancy$lightBuffer`: GpuBuffer?
    var `vibrancy$shadowBuffer`: GpuBuffer?
    var `vibrancy$gridBuffer`: GpuBuffer?

    fun `vibrancy$clear`() {
        `vibrancy$lightBuffer` = null
        `vibrancy$shadowBuffer` = null
        `vibrancy$gridBuffer` = null
    }
}