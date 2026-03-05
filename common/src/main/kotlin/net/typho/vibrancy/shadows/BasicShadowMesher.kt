package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.typho.vibrancy.LightManager
import java.util.*
import java.util.function.Consumer

open class BasicShadowMesher : ShadowMesher {
    val shadowFaces = LinkedList<LightFace>()
    val lightFaces = LinkedList<LightFace>()

    override fun submit(
        manager: LightManager,
        level: Level,
        pos: BlockPos,
        random: RandomSource,
        predicate: ShadowPredicate
    ) {
        val block = level.getBlockState(pos)

        if (predicate.shouldCastBlock(block, level, pos)) {
            val shadow = predicate.isInShadowRange(pos)
            val light = predicate.isInLightRange(pos)

            if (shadow || light) {
                ShadowMesher.collectLightFaces(manager, block, level, pos, predicate) { dir, face ->
                    if (shadow) {
                        shadowFaces.add(face)
                    }

                    if (light) {
                        lightFaces.add(face)
                    }
                }
            }
        }

        /*
        val fluid = level.getFluidState(pos)

        if (predicate.shouldCastFluid(fluid, level, pos)) {
            val shadow = predicate.isInShadowRange(pos)
            val light = predicate.isInLightRange(pos)

            if (shadow || light) {
                val consumer = LightFace.Consumer(pos)

                Minecraft.getInstance().blockRenderer.renderLiquid(
                    pos,
                    level,
                    consumer.toMojang(),
                    block,
                    fluid
                )

                if (shadow) {
                    shadowFaces.addAll(consumer.end())
                }

                if (light) {
                    lightFaces.addAll(consumer.end())
                }
            }
        }
         */
    }

    override fun finish(
        manager: LightManager,
        predicate: ShadowPredicate,
        level: Level,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>
    ) {
        shadowFaces.forEach(shadowOut)
        lightFaces.forEach(lightOut)
    }
}