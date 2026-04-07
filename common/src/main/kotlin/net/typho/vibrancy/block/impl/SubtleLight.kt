package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.vibrancy.util.PointLight

open class SubtleLight(
    @JvmField
    val color: AbstractVec3<Float>,
    @JvmField
    val offset: AbstractVec3<Float>,
    override val pos: AbstractVec3<Int>
) : PointLight {
    constructor(info: SubtleLightInfo, state: BlockState, pos: AbstractVec3<Int>) : this(
        info.color.apply(state) * info.brightness.apply(state),
        info.offset.apply(state),
        pos
    )

    override val absolutePos: AbstractVec3<Float>
        get() = pos.toFloat() + offset
    override val boundingBox: AbstractRect3<Int>
        get() = NeoRect3i(
            pos - 3,
            pos + 3,
        )
    override val shadowBox: AbstractRect3<Int>
        get() = NeoRect3i(
            pos - 1,
            pos + 1,
        )
}