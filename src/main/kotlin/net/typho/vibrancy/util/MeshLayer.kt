package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch

data class MeshLayer(
    @JvmField
    val batch: MultiDrawBatch,
    @JvmField
    val capacity: Int
)