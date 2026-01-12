package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.StateHolder
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

class RayPointLightInfo(
    val color: StateFunction<Vector3f>,
    val radius: StateFunction<Float>,
    val brightness: StateFunction<Float>,
    val offset: StateFunction<Vector3f>,
    val enabled: StateFunction<Boolean>
) : BlockLightInfo {
    override fun createBlockLight(state: StateHolder<*, *>, pos: BlockPos): RayPointLight? {
        if (!enabled.apply(state)) {
            return null
        }

        return RayPointLight(this, state, pos)
    }

    override fun type() = RayPointLightType
}