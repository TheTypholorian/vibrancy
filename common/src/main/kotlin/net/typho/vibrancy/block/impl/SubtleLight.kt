package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

open class SubtleLight(
    @JvmField
    val color: Vector3f,
    @JvmField
    val offset: Vector3f,
    @JvmField
    val blockPos: BlockPos
) {
    constructor(info: SubtleLightInfo, state: BlockState, pos: BlockPos) : this(
        info.color.apply(state).mul(info.brightness.apply(state), Vector3f()),
        info.offset.apply(state),
        pos
    )

    val absolutePos: Vector3f
        get() = Vector3f(blockPos.x.toFloat(), blockPos.y.toFloat(), blockPos.z.toFloat()).add(offset)
    val boundingBox: AABB
        get() {
            return AABB.ofSize(
                Vec3(absolutePos.x.toDouble(), absolutePos.y.toDouble(), absolutePos.z.toDouble()),
                8.0,
                8.0,
                8.0
            )
        }
}