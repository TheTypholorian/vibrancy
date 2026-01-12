package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLight
import org.joml.Vector3f
import kotlin.math.ceil

class RayPointLight(
    val color: Vector3f,
    val radius: Float,
    val offset: Vector3f,
    val pos: BlockPos
) : BlockLight<RayPointLightInfo> {
    constructor(info: RayPointLightInfo, state: StateHolder<*, *>, pos: BlockPos) : this(
        info.color.apply(state).mul(info.brightness.apply(state), Vector3f()),
        info.radius.apply(state),
        info.offset.apply(state),
        pos
    )

    override fun getBlockPos() = pos

    override fun getAbsolutePos(): Vector3f {
        return Vector3f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()).add(offset)
    }

    override fun getCullingBox(): AABB {
        val radius2 = (radius * 2).toDouble()
        return AABB.ofSize(Vec3(getAbsolutePos()), radius2, radius2, radius2)
    }

    override fun getShadowBox(): AABB {
        val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.shadowRadius.toFloat())).toDouble()
        return AABB.ofSize(Vec3(getAbsolutePos()), shadowRadius, shadowRadius, shadowRadius)
    }

    override fun getType() = RayPointLightType

    override fun shouldRaytrace() = true

    override fun numShadows(): Int {
        TODO("Not yet implemented")
    }

    override fun numAsyncTasksActive(): Int {
        TODO("Not yet implemented")
    }

    override fun numEntities(): Int {
        TODO("Not yet implemented")
    }

    override fun numBlockEntities(): Int {
        TODO("Not yet implemented")
    }

    override fun free() {
        TODO("Not yet implemented")
    }
}