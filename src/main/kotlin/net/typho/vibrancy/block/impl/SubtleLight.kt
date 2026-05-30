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
    val flicker: Float,
    @JvmField
    val shape: Int,
    @JvmField
    val offset: IVec3<Float>,
    override val pos: IVec3<Int>
) : PointLight {
    companion object {
        const val SQUARE_SHAPE = 0
        const val CIRCLE_SHAPE = 1
    }

    constructor(info: SubtleLightInfo, state: BlockState, pos: IVec3<Int>) : this(
        info.color(state) * info.brightness(state),
        info.flicker(state),
        info.shape(state),
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