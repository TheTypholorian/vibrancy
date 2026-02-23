package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

class RayPointLightInfo(
    val color: StateFunction<Vector3f>,
    val radius: StateFunction<Float>,
    val brightness: StateFunction<Float>,
    val offset: StateFunction<Vector3f>,
    val enabled: StateFunction<Boolean>
) : BlockLightInfo<RayPointLightInfo, RayPointLight> {
    override fun createBlockLight(manager: LightManager, level: Level, state: BlockState, pos: BlockPos): RayPointLight? {
        if (!enabled.apply(state)) {
            return null
        }

        return RayPointLight(this, state, pos)
    }

    override fun type() = RayPointLightType

    override fun shouldCastShadow(manager: LightManager, level: Level, state: BlockState, pos: BlockPos): Boolean {
        return BlockUtil.INSTANCE.isSolidRender(state, pos, level)
    }
}