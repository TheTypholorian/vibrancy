package net.typho.vibrancy.shadows

import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.plus
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.Vibrancy.isPointingTowardsInclusive
import java.util.function.Consumer

class FloodFillMesher(
    @JvmField
    val pos: AbstractVec3<Int>,
    @JvmField
    val cursors: MutableList<AbstractVec3<Int>> = arrayListOf(pos),
    @JvmField
    val checked: MutableSet<AbstractVec3<Int>> = hashSetOf(),
    @JvmField
    val faces: MutableList<LightFace> = arrayListOf()
) : ShadowMesher {
    fun markAllDirty() {
        cursors.clear()
        cursors.add(pos)
        checked.clear()
        faces.clear()
    }

    fun markDirty(pos: AbstractVec3<Int>): Boolean {
        if (checked.contains(pos)) {
            cursors.add(pos)
            faces.removeIf { it.blockPos == pos }
            return true
        } else {
            return false
        }
    }

    override fun submit(
        manager: LightManager,
        level: Level,
        predicate: ShadowPredicate,
        atlas: NeoAtlas,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    ) {
        fun collect(pos: AbstractVec3<Int>, state: BlockState) {
            ShadowMesher.collectLightFaces(
                manager,
                state,
                level,
                pos,
                atlas,
                { predicate.shouldCastFace(it, level, pos, state) }
            ) { dir, face -> faces.add(face) }
        }

        while (cursors.isNotEmpty()) {
            val cursor = cursors.removeLast()

            if (predicate.shouldCastBlock(level, cursor)) {
                collect(cursor, level.getBlockState(cursor.blockPos))
            }

            for (direction in NeoDirection.entries) {
                val pos = cursor + direction

                if (direction.isPointingTowardsInclusive(this.pos, cursor) && checked.add(pos)) {
                    val state = level.getBlockState(pos.blockPos)

                    if (predicate.shouldCastBlock(level, pos, state) && predicate.isInLightRange(pos)) {
                        if (BlockUtil.INSTANCE.isSolidRender(state, pos, level)) {
                            collect(pos, state)
                        } else {
                            cursors.add(pos)
                        }
                    }
                }
            }
        }

        faces.forEach {
            if (predicate.isInShadowRange(it.blockPos) && !level.getBlockState(it.blockPos.blockPos).`is`(Vibrancy.noShadowsTag)) {
                shadowOut.accept(it)
            }

            lightOut.accept(it)
        }
    }
}