package net.typho.vibrancy.light

import net.minecraft.client.Camera
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.gl.GlStack

interface Light {
    fun render(manager: LightManager, raytrace: Boolean, stack: GlStack)

    fun shouldCastBlock(state: BlockState, level: Level, pos: BlockPos): Boolean = state.isSolidRender(level, pos) || !BlockLightInfo.has(state.block)

    fun getCullingBox(): AABB?

    fun testCullingDistance(camera: Camera, chunks: Int): Boolean
}