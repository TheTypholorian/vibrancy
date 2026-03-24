package net.typho.vibrancy.shadows

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.typho.big_shot_lib.api.client.util.quads.NeoAtlas
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import java.util.function.Consumer

class SkyLightMesher(
    @JvmField
    val pos: ChunkPos
) : ShadowMesher {
    override fun submit(
        manager: LightManager,
        level: Level,
        predicate: ShadowPredicate,
        atlas: NeoAtlas,
        shadowOut: Consumer<LightFace>,
        lightOut: Consumer<LightFace>,
        splitLargeLightFaces: Boolean
    ) {
        val faces = arrayListOf<LightFace>()

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

        val chunk = level.getChunk(pos.x, pos.z)

        repeat(16) { x ->
            repeat(16) { z ->
                val height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                var y = height

                while (y > chunk.minBuildHeight) {
                    val pos = BlockPos(x + pos.minBlockX, y, z + pos.minBlockZ)
                    val state = chunk.getBlockState(pos)

                    // !BlockUtil.INSTANCE.isSolidRender(state, pos, level) &&
                    if (predicate.shouldCastBlock(level, pos, state)) {
                        collect(pos, state)
                    }

                    if (!state.propagatesSkylightDown(level, pos)) {
                        break
                    }

                    y--
                }
            }
        }

        /*
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
         */

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