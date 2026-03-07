package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.shadows.ShadowPredicate
import net.typho.vibrancy.util.PointLight
import org.joml.Vector3f

open class SubtleLight(
    @JvmField
    val color: Vector3f,
    @JvmField
    val offset: Vector3f,
    override val blockPos: BlockPos
) : PointLight {
    companion object {
        @JvmField
        val SHADOW_PREDICATE = object : ShadowPredicate {
            override fun shouldCastBlock(
                level: Level,
                pos: BlockPos,
                state: BlockState
            ): Boolean {
                return true
            }

            override fun shouldCastFace(
                face: Direction?,
                level: Level,
                pos: BlockPos,
                state: BlockState
            ): Boolean {
                if (face == null) {
                    return true
                }

                return BlockUtil.INSTANCE.shouldRenderFace(level, pos, face, state)
            }

            override fun isInLightRange(pos: BlockPos): Boolean {
                return true
            }

            override fun isInShadowRange(pos: BlockPos): Boolean {
                return false
            }
        }
    }

    constructor(info: SubtleLightInfo, state: BlockState, pos: BlockPos) : this(
        info.color.apply(state).mul(info.brightness.apply(state), Vector3f()),
        Vector3f(info.offset.apply(state)),
        pos
    )

    override val absolutePos: Vector3f
        get() = Vector3f(blockPos.x.toFloat(), blockPos.y.toFloat(), blockPos.z.toFloat()).add(offset)
    override val boundingBox: AABB
        get() {
            return AABB.ofSize(
                Vec3(absolutePos.x.toDouble(), absolutePos.y.toDouble(), absolutePos.z.toDouble()),
                3.0,
                3.0,
                3.0
            )
        }
    override val shadowBox: BlockBox
        get() = BlockBox.of(
            BlockPos(blockPos.x - 1, blockPos.y - 1, blockPos.z - 1),
            BlockPos(blockPos.x + 1, blockPos.y + 1, blockPos.z + 1)
        )
    override val shadowPredicate: ShadowPredicate = SHADOW_PREDICATE

    fun shouldRender(chunk: SubtleLightStorage.Chunk): Boolean {
        return Direction.entries.any { !chunk.map.containsKey(blockPos.relative(it)) }
    }
}