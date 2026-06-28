package net.typho.vibrancy

import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer

interface RenderRegionExtension {
    var `vibrancy$lightBuffer`: GpuBuffer?
    var `vibrancy$shadowBuffer`: GpuBuffer?
}