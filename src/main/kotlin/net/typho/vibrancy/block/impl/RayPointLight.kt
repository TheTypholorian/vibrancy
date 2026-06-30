package net.typho.vibrancy.block.impl

//? if sable {
/*import dev.ryanhcode.sable.companion.SableCompanion
*///? }

import net.minecraft.core.SectionPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.math.IRect3
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.util.PointLight
import org.lwjgl.system.NativeResource

open class RayPointLight(
    level: Level,
    @JvmField
    val color: IVec3<Float>,
    @JvmField
    val flicker: Float,
    @JvmField
    val radius: Int,
    @JvmField
    val offset: IVec3<Float>,
    override val pos: IVec3<Int>
) : PointLight, NativeResource {
    /*
    companion object {
        @JvmStatic
        fun drawState(shader: Identifier, uniforms: GlBoundProgram.() -> Unit) = GlDrawState.Basic(
            blend = GlBlendShard.Enabled(
                BlendFunction.Basic(
                    GlBlendingFactor.DST_COLOR,
                    GlBlendingFactor.ZERO
                ),
                GlBlendEquation.ADD
            ),
            shader = GlShaderShard.FromLocation(
                shader,
                uniforms
            )
        )
    }
     */

    //? if sable {
    /*override val absolutePos: IVec3<Float>
        get() = SableCompanion.INSTANCE.getContainingClient((pos.toDouble() + offset.toDouble()).toJOML())?.let { IVec3(it.renderPose(Vibrancy.tickDelta).transformPosition((pos.toDouble() + offset.toDouble()).toJOML())).toFloat() } ?: (pos.toFloat() + offset)
    val absoluteBlockPos: IVec3<Float>
        get() = SableCompanion.INSTANCE.getContainingClient(pos.toDouble().toJOML())?.let { IVec3(
            it.renderPose(
                Vibrancy.tickDelta
            ).transformPosition(pos.toDouble().toJOML())
        ).toFloat() } ?: pos.toFloat()
    *///? } else {
    override val absolutePos: IVec3<Float>
        get() = pos.toFloat() + offset
    val absoluteBlockPos: IVec3<Float>
        get() = pos.toFloat()
    //? }
    override val boundingBox: IRect3<Int> = IRect3(pos - radius, pos + radius)
    override var shadowBox: IRect3<Int> = createShadowBox()
    @JvmField
    val sections: List<SectionPos> = SectionPos.betweenClosedStream(
        SectionPos.blockToSectionCoord(boundingBox.min.x),
        SectionPos.blockToSectionCoord(boundingBox.min.y).coerceAtLeast(level.minSectionY).coerceAtMost(level.maxSectionY),
        SectionPos.blockToSectionCoord(boundingBox.min.z),
        SectionPos.blockToSectionCoord(boundingBox.max.x),
        SectionPos.blockToSectionCoord(boundingBox.max.y).coerceAtLeast(level.minSectionY).coerceAtMost(level.maxSectionY),
        SectionPos.blockToSectionCoord(boundingBox.max.z)
    ).toList()
    @JvmField
    val sectionPos = SectionPos.of(absolutePos.toBlockPos())
    val shadowRadius: Int
        get() = radius.coerceAtMost(VibrancyConfig.rayLightShadowRadius)

    fun createShadowBox(): IRect3<Int> {
        val shadowRadius = shadowRadius
        return IRect3(pos - shadowRadius, pos + shadowRadius)
    }

    constructor(level: Level, info: RayPointLightInfo, state: BlockState, pos: IVec3<Int>) : this(
        level,
        info.color(state) * info.brightness(state),
        info.flicker(state),
        info.radius(state),
        info.offset(state),
        pos
    )

    fun reload() {
    }

    override fun free() {
    }

    fun numActiveTasks() = 0 // TODO
}