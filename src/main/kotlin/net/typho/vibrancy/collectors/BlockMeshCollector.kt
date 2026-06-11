package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.util.SectionMeshCache

interface BlockMeshCollector {
    fun scan(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        predicate: Predicate
    ): Boolean

    fun mesh(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        consumer: Consumer
    )

    interface Predicate {
        fun isBlockTransparent(
            level: Level,
            pos: BlockPos,
            state: BlockState
        ): Boolean = !BlockUtil.INSTANCE.isSolidRender(state, pos, level)

        fun shouldCastBlock(
            level: Level,
            pos: BlockPos,
            state: BlockState
        ): Boolean

        fun shouldCastFace(
            level: Level,
            pos: BlockPos,
            state: BlockState,
            face: LightFace
        ): Boolean

        fun requiresScan(
            level: Level,
            pos: BlockPos,
            old: BlockState,
            new: BlockState
        ): Boolean {
            return isBlockTransparent(level, pos, old) != isBlockTransparent(level, pos, new) || shouldCastBlock(level, pos, old) != shouldCastBlock(level, pos, new)
        }

        infix fun and(other: Predicate): Predicate {
            val parent = this
            return object : Predicate {
                override fun shouldCastBlock(
                    level: Level,
                    pos: BlockPos,
                    state: BlockState
                ): Boolean {
                    return parent.shouldCastBlock(level, pos, state) && other.shouldCastBlock(level, pos, state)
                }

                override fun shouldCastFace(
                    level: Level,
                    pos: BlockPos,
                    state: BlockState,
                    face: LightFace
                ): Boolean {
                    return parent.shouldCastFace(level, pos, state, face) && other.shouldCastFace(level, pos, state, face)
                }
            }
        }

        infix fun or(other: Predicate): Predicate {
            val parent = this
            return object : Predicate {
                override fun shouldCastBlock(
                    level: Level,
                    pos: BlockPos,
                    state: BlockState
                ): Boolean {
                    return parent.shouldCastBlock(level, pos, state) || other.shouldCastBlock(level, pos, state)
                }

                override fun shouldCastFace(
                    level: Level,
                    pos: BlockPos,
                    state: BlockState,
                    face: LightFace
                ): Boolean {
                    return parent.shouldCastFace(level, pos, state, face) || other.shouldCastFace(level, pos, state, face)
                }
            }
        }
    }

    interface Consumer {
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
            consumer: Consumer
        ) {
            val section = SectionPos.of(pos)
            val cache = caches.computeIfAbsent(section) {
                synchronized(manager.sectionLock) {
                    manager.sectionMeshCaches[it]
                }
            }

            if (cache != null) {
                cache[pos]?.collect(consumer, section, pos)
            }
        }

        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            state: BlockState,
            level: Level,
            pos: BlockPos,
            consumer: Consumer
        ) {
            val section = SectionPos.of(pos)
            val cache = synchronized(manager.sectionLock) { manager.sectionMeshCaches[section] }

            if (cache != null) {
                cache[pos]?.collect(consumer, section, pos)
            }
        }

        @JvmStatic
        fun collectLightFaces(
            manager: LightManager,
            state: BlockState,
            level: Level,
            pos: BlockPos,
            transmute: (face: LightFace) -> LightFace,
            consumer: Consumer
        ) {
            val section = SectionPos.of(pos)
            val cache = synchronized(manager.sectionLock) {manager.sectionMeshCaches[section] }

            if (cache != null) {
                cache[pos]?.collect(consumer, section, pos, transmute)
            }
        }
    }
}