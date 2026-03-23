package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.util.quads.NeoAtlas
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.Vibrancy.isPointingTowardsInclusive
import java.util.function.Consumer

class FloodFillMesher(
    @JvmField
    val pos: BlockPos,
    @JvmField
    val cursors: MutableSet<BlockPos> = hashSetOf(pos),
    @JvmField
    val checked: MutableSet<BlockPos> = hashSetOf(),
    @JvmField
    val faces: MutableList<LightFace> = arrayListOf()
) : ShadowMesher {
    fun markAllDirty() {
        cursors.clear()
        cursors.add(pos)
        checked.clear()
        faces.clear()
    }

    fun markDirty(pos: BlockPos) {
        val remove = setOf(
            pos,
            pos.above(),
            pos.below(),
            pos.north(),
            pos.south(),
            pos.west(),
            pos.east()
        )

        checked.addAll(remove)
        cursors.addAll(remove)
        faces.removeIf { remove.contains(it.blockPos) }
    }

    override fun submit(
        manager: LightManager,
        level: Level,
        predicate: ShadowPredicate,
        atlas: NeoAtlas,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>,
        splitLargeLightFaces: Boolean
    ) {
        fun collect(pos: BlockPos, state: BlockState) {
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
            val cursor = cursors.first()
            cursors.remove(cursor)

            collect(cursor, level.getBlockState(cursor))

            for (direction in Direction.entries) {
                val pos = cursor.relative(direction)

                if (direction.isPointingTowardsInclusive(this.pos, cursor) && checked.add(pos)) {
                    val state = level.getBlockState(pos)

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
            if (predicate.isInShadowRange(it.blockPos) && !level.getBlockState(it.blockPos).`is`(Vibrancy.noShadowsTag)) {
                shadowOut.accept(it)
            }

            if (splitLargeLightFaces) {
                it.split(16).forEach(lightOut::accept)
            } else {
                lightOut.accept(it)
            }
        }
    }
}