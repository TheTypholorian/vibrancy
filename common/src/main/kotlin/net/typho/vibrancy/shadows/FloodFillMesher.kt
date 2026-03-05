package net.typho.vibrancy.shadows

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.typho.vibrancy.LightManager
import java.util.function.Consumer

class FloodFillMesher(
    @JvmField
    val box: BlockBox,
    @JvmField
    val pos: BlockPos
) : ShadowMesher {
    override fun submit(
        manager: LightManager,
        level: Level,
        random: RandomSource,
        predicate: ShadowPredicate,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    ) {
        val cursors = ArrayList<BlockPos>()
        val checked = HashSet<BlockPos>()

        cursors.add(pos)

        while (cursors.isNotEmpty()) {
            val cursor = cursors.removeFirst()

            for (direction in Direction.entries) {
                val pos = cursor.relative(direction)

                if (box.contains(pos) && checked.add(pos)) {
                    val state = level.getBlockState(pos)

                    if (state.isAir) {
                        cursors.add(pos)
                    } else {
                        val shadow = predicate.isInShadowRange(pos)
                        val light = predicate.isInLightRange(pos)

                        if (shadow || light) {
                            ShadowMesher.collectLightFaces(manager, state, level, pos, predicate) { dir, face ->
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