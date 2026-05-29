package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.util.SectionMeshCache

interface BlockMeshCollector {
    fun submit(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        vararg consumers: Consumer
    ): Boolean

    interface Predicate {
        fun isBlockTransparent(
            level: Level,
            pos: BlockPos,
            state: BlockState
        ): Boolean = !BlockUtil.INSTANCE.isSolidRender(state, pos, level)

        fun shouldCastBlock(
            level: Level,
            pos: BlockPos,
            state: BlockState?
        ): Boolean
    }

    interface Consumer {
        val predicate: Predicate

        fun collect(faces: Iterable<LightFace>, section: SectionPos, block: BlockPos, translucent: Boolean)
    }

    interface FaceOrigin {
        val pos: IVec3<Int>

        data class Block(
            @JvmField
            val block: BlockState,
            override val pos: IVec3<Int>
        ) : FaceOrigin

        data class Fluid(
            @JvmField
            val fluid: FluidState,
            override val pos: IVec3<Int>
        ) : FaceOrigin
    }

    companion object {
        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            caches: MutableMap<SectionPos, SectionMeshCache?>,
            state: BlockState,
            level: Level,
            pos: BlockPos,
            vararg consumers: Consumer
        ) {
            val section = SectionPos.of(pos)
            val cache = caches.computeIfAbsent(section) { manager.sectionMeshCaches[it] }

            if (cache != null) {
                val model = cache[pos]
                consumers.forEach {
                    if (it.predicate.shouldCastBlock(level, pos, state)) {
                        it.collect(model.solidFaces, section, pos, false)
                        it.collect(model.translucentFaces, section, pos, true)
                    }
                }
            }
        }

        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            state: BlockState,
            level: Level,
            pos: BlockPos,
            vararg consumers: Consumer
        ) {
            val section = SectionPos.of(pos)
            val cache = manager.sectionMeshCaches[section]

            if (cache != null) {
                val model = cache[pos]
                consumers.forEach {
                    if (it.predicate.shouldCastBlock(level, pos, state)) {
                        it.collect(model.solidFaces, section, pos, false)
                        it.collect(model.translucentFaces, section, pos, true)
                    }
                }
            }
        }

        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            state: BlockState,
            level: Level,
            pos: BlockPos,
            transmute: (face: LightFace) -> LightFace,
            vararg consumers: Consumer
        ) {
            val section = SectionPos.of(pos)
            val cache = manager.sectionMeshCaches[section]

            if (cache != null) {
                val model = cache[pos]
                consumers.forEach {
                    if (it.predicate.shouldCastBlock(level, pos, state)) {
                        it.collect(model.solidFaces.map(transmute), section, pos, false)
                        it.collect(model.translucentFaces.map(transmute), section, pos, true)
                    }
                }
            }
        }
    }
}