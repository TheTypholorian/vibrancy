package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import java.util.function.Consumer

class FloodFillMesher(
    @JvmField
    val pos: BlockPos
) : ShadowMesher {
    override fun submit(
        manager: LightManager,
        level: Level,
        predicate: ShadowPredicate,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    ) {
        val cursors = ArrayList<BlockPos>()
        val checked = HashSet<BlockPos>()

        cursors.add(pos)

        while (cursors.isNotEmpty()) {
            val cursor = cursors.removeLast()

            for (direction in Direction.entries) {
                val pos = cursor.relative(direction)

                if (checked.add(pos)) {
                    val state = level.getBlockState(pos)

                    if (predicate.shouldCastBlock(level, pos, state)) {
                        val shadow = predicate.isInShadowRange(pos)
                        val light = predicate.isInLightRange(pos)

                        if (shadow || light) {
                            if (!BlockUtil.INSTANCE.isSolidRender(state, pos, level)) {
                                cursors.add(pos)
                            }

                            ShadowMesher.collectLightFaces(
                                manager,
                                state,
                                level,
                                pos,
                                { predicate.shouldCastFace(it, level, pos, state) }
                            ) { dir, face ->
                                if (shadow) {
                                    shadowOut.accept(face)
                                }

                                if (light) {
                                    lightOut.accept(face)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}