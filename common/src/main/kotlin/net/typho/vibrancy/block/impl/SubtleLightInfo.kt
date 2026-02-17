package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.services.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

class SubtleLightInfo(
    val color: StateFunction<Vector3f>,
    val brightness: StateFunction<Float>,
    val offset: StateFunction<Vector3f>,
    val enabled: StateFunction<Boolean>
) : BlockLightInfo<SubtleLightInfo, SubtleLight> {
    override fun createBlockLight(manager: LightManager, level: Level, state: BlockState, pos: BlockPos): SubtleLight? {
        if (!enabled.apply(state)) {
            return null
        }

        for (direction in Direction.entries) {
            val rPos = pos.relative(direction)
            val rState = level.getBlockState(rPos)

            if (!BlockUtil.INSTANCE.isSolidRender(rState, rPos, level) && !BlockLightRegistry.has(rState.block)) {
                return SubtleLight(this, state, pos)
            }
        }

        return null
    }

    override fun type() = SubtleLightType
}