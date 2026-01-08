package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

interface ShadowMesher {
    fun submit(
        state: BlockState,
        level: Level,
        pos: BlockPos
    )

    fun upload(
        vbo: VertexBuffer
    )
}