package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.StateHolder
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

class SubtleLightInfo(
    val color: StateFunction<Vector3f>,
    val brightness: StateFunction<Float>,
    val offset: StateFunction<Vector3f>,
    val enabled: StateFunction<Boolean>
) : BlockLightInfo {
    override fun createBlockLight(manager: LightManager, state: StateHolder<*, *>, pos: BlockPos): SubtleLight? {
        if (!enabled.apply(state)) {
            return null
        }

        SubtleLightType.dirty = true

        return SubtleLight(this, state, pos)
    }

    override fun type() = RayPointLightType
}