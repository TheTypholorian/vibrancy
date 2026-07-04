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
import kotlin.math.ceil

open class RayPointLight(
    level: Level,
    @JvmField
    val color: IVec3<Float>,
    @JvmField
    val flicker: Float,
    @JvmField
    val radius: Float,
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
    override val boundingBox: IRect3<Float> = IRect3(pos.toFloat() - radius, pos.toFloat() + radius)
    @JvmField
    val sections: List<SectionPos> = SectionPos.betweenClosedStream(
        SectionPos.blockToSectionCoord(boundingBox.min.x.toInt()),
        SectionPos.blockToSectionCoord(boundingBox.min.y.toInt()).coerceAtLeast(level.minSectionY).coerceAtMost(level.maxSectionY),
        SectionPos.blockToSectionCoord(boundingBox.min.z.toInt()),
        SectionPos.blockToSectionCoord(ceil(boundingBox.max.x).toInt()),
        SectionPos.blockToSectionCoord(ceil(boundingBox.max.y).toInt()).coerceAtLeast(level.minSectionY).coerceAtMost(level.maxSectionY),
        SectionPos.blockToSectionCoord(ceil(boundingBox.max.z).toInt())
    ).toList()
    @JvmField
    val sectionPos = SectionPos.of(absolutePos.toBlockPos())
    val shadowRadius: Int
        get() = ceil(radius).toInt().coerceAtMost(VibrancyConfig.rayLightShadowRadius).coerceAtLeast(0)

    fun getShadowBox(voxelShift: Int): IRect3<Int> {
        val shadowRadius = shadowRadius ushr voxelShift
        return IRect3(pos - shadowRadius, pos + shadowRadius)
    }

    fun shadowGridCellRelativePosToWorldPos(cell: IVec3<Int>, voxelShift: Int): IVec3<Int> {
        val x = cell.x shl voxelShift
        val y = cell.y shl voxelShift
        val z = cell.z shl voxelShift
        return pos.plus(x, y, z)
    }

    fun shadowGridCellPosToWorldPos(cell: IVec3<Int>, voxelShift: Int): IVec3<Int> {
        return shadowGridCellRelativePosToWorldPos(cell - pos, voxelShift)
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