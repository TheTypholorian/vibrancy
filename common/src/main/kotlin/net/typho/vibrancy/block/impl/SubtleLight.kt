package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.util.PointLight

open class SubtleLight(
    @JvmField
    val color: IVec3<Float>,
    @JvmField
    val offset: IVec3<Float>,
    override val pos: IVec3<Int>
) : PointLight {
    constructor(info: SubtleLightInfo, state: BlockState, pos: IVec3<Int>) : this(
        info.color(state) * info.brightness(state),
        info.offset(state),
        pos
    )

    override val absolutePos: IVec3<Float>
        get() = pos.toFloat() + offset
    override val boundingBox: AbstractRect3<Int>
        get() = NeoRect3i(
            pos - 1,
            pos + 1,
        )
    override val shadowBox: AbstractRect3<Int>
        get() = NeoRect3i(
            pos - 1,
            pos + 1,
        )
}