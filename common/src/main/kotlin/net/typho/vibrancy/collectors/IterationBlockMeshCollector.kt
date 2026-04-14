package net.typho.vibrancy.collectors

import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.vibrancy.LightManager

class IterationBlockMeshCollector(
    @JvmField
    val blocks: Iterable<IVec3<Int>>
) : BlockMeshCollector {
    override fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        vararg consumers: BlockMeshCollector.Consumer
    ) {
        for (pos in blocks) {
            BlockMeshCollector.collectLightFaces(
                manager,
                level.getBlockState(pos.blockPos),
                level,
                pos,
                atlas,
                true,
                *consumers
            )
        }
    }
}