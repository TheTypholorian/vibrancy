package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.BlockLight
import org.joml.Vector3f

class SubtleLight(
    val color: Vector3f,
    val offset: Vector3f,
    val pos: BlockPos
) : BlockLight<SubtleLightInfo, SubtleLight> {
    constructor(info: SubtleLightInfo, state: StateHolder<*, *>, pos: BlockPos) : this(
        info.color.apply(state).mul(info.brightness.apply(state), Vector3f()),
        info.offset.apply(state),
        pos
    )

    override fun getBoundingBox(): AABB = AABB.ofSize(
        Vec3(getAbsolutePos()),
        8.0,
        8.0,
        8.0
    )

    override fun rebuildShadows(manager: LightManager, fullQuality: Boolean) {
    }

    override fun getType() = SubtleLightType

    override fun shouldRaytrace(manager: LightManager) = false

    override fun free(manager: LightManager) {
    }

    override fun getBlockPos() = pos

    override fun getAbsolutePos(): Vector3f {
        return Vector3f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()).add(offset)
    }

    override fun getShadowBox(fullQuality: Boolean) = null

    override fun getShadowPredicate(fullQuality: Boolean) = null

    override fun shouldRender(manager: LightManager): Boolean {
        for (direction in Direction.entries) {
            if (manager.getLevel().getBlockState(pos.relative(direction)).isAir) {
                return true
            }
        }

        return false
    }
}