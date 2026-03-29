package net.typho.vibrancy.block.impl

import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.shadows.ShadowPredicate
import net.typho.vibrancy.util.PointLight

open class SubtleLight(
    @JvmField
    val color: AbstractVec3<Float>,
    @JvmField
    val offset: AbstractVec3<Float>,
    override val pos: AbstractVec3<Int>
) : PointLight {
    companion object {
        @JvmField
        val SHADOW_PREDICATE = object : ShadowPredicate {
            override fun shouldCastBlock(
                level: Level,
                pos: AbstractVec3<Int>,
                state: BlockState
            ): Boolean {
                return true
            }

            override fun shouldCastFace(
                face: NeoDirection?,
                level: Level,
                pos: AbstractVec3<Int>,
                state: BlockState
            ): Boolean {
                if (face == null) {
                    return true
                }

                return BlockUtil.INSTANCE.shouldRenderFace(level, pos, face, state)
            }

            override fun isInLightRange(pos: AbstractVec3<Int>): Boolean {
                return true
            }

            override fun isInShadowRange(pos: AbstractVec3<Int>): Boolean {
                return false
            }
        }
    }

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
    override val shadowPredicate: ShadowPredicate = SHADOW_PREDICATE
}