package net.typho.vibrancy.shadows

import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.plus
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy.isPointingTowardsInclusive

class FloodFillMesher(
    @JvmField
    val pos: AbstractVec3<Int>,
    @JvmField
    val dirty: MutableList<AbstractVec3<Int>> = arrayListOf(),
    @JvmField
    val checked: MutableSet<AbstractVec3<Int>> = hashSetOf()
) : ShadowMesher {
    init {
        dirty.add(pos)
    }

    fun markAllDirty() {
        dirty.clear()
        dirty.add(pos)
        checked.clear()
    }

    fun markDirty(pos: AbstractVec3<Int>): Boolean {
        if (checked.contains(pos)) {
            dirty.add(pos)
            return true
        } else {
            return false
        }
    }

    override fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        predicate: LightFacePredicate,
        out: (face: LightFace) -> Unit
    ) {
        var cursors = dirty.toMutableList()
        var newCursors = arrayListOf<AbstractVec3<Int>>()

        do {
            while (cursors.isNotEmpty()) {
                val cursor = cursors.removeLast()

                if (predicate.shouldCastBlock(level, cursor)) {
                    checked.add(cursor)
                }

                for (direction in NeoDirection.entries) {
                    val pos = cursor + direction

                    if (direction.isPointingTowardsInclusive(this.pos, cursor) && checked.add(pos)) {
                        val state = level.getBlockState(pos.blockPos)

                        if (predicate.shouldCastBlock(level, pos, state)) {
                            if (!BlockUtil.INSTANCE.isSolidRender(state, pos, level)) {
                                newCursors.add(pos)
                            }
                        }
                    }
                }
            }

            cursors = newCursors
            newCursors = arrayListOf()
        } while (cursors.isNotEmpty())

        for (pos in checked) {
            val state = level.getBlockState(pos.blockPos)
            ShadowMesher.collectLightFaces(
                manager,
                state,
                level,
                pos,
                atlas,
                { predicate.shouldCastFace(it, level, pos, state) },
                { dir, face -> out(face) }
            )
        }
    }
}