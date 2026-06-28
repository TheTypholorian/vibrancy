package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy.isPointingTowardsInclusive

class FloodFillBlockMeshCollector(
    @JvmField
    val pos: BlockPos
) : BlockMeshCollector {
    class Cache(
        @JvmField
        var checked: MutableSet<BlockPos> = hashSetOf(),
        @JvmField
        var collect: MutableSet<BlockPos> = hashSetOf()
    ) {
        constructor(old: Cache) : this(HashSet(old.checked), HashSet(old.collect))
    }

    @JvmField
    val dirty: MutableList<BlockPos> = arrayListOf(pos)
    @JvmField
    var cache: Cache = Cache()
    var blockEntities: MutableSet<BlockPos> = hashSetOf()
        private set
    @JvmField
    var allDirty = true

    fun markAllDirty() {
        if (!allDirty) {
            allDirty = true
            dirty.clear()
            dirty.add(pos)
            blockEntities = hashSetOf()
        }
    }

    fun markDirty(pos: BlockPos): Boolean {
        if (cache.checked.contains(pos)) {
            dirty.add(pos)
            return true
        } else {
            return false
        }
    }

    override fun scan(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        predicate: BlockMeshCollector.Predicate
    ): Boolean {
        val cache = if (allDirty) Cache() else Cache(cache)
        var cursors = dirty.toMutableList()
        var newCursors = arrayListOf<BlockPos>()
        val blockEntities = hashSetOf<BlockPos>()

        do {
            while (cursors.isNotEmpty()) {
                if (isCancelled()) {
                    return false
                }

                val cursor = cursors.removeLast()
                val state = level.getBlockState(cursor)

                if (predicate.shouldCastBlock(level, cursor, state)) {
                    cache.checked.add(cursor)
                    cache.collect.add(cursor)

                    if (level.getBlockEntity(cursor) != null) {
                        blockEntities.add(cursor)
                    }
                }

                for (direction in Direction.entries) {
                    if (direction.isPointingTowardsInclusive(this.pos, cursor)) {
                        val check = cursor.relative(direction)

                        if (cache.checked.add(check)) {
                            val state = level.getBlockState(check)

                            if (predicate.shouldCastBlock(level, check, state)) {
                                cache.collect.add(check)

                                if (level.getBlockEntity(check) != null) {
                                    blockEntities.add(check)
                                }

                                if (predicate.isBlockTransparent(level, check, state)) {
                                    newCursors.add(check)
                                }
                            }
                        }
                    }
                }
            }

            cursors = newCursors
            newCursors = arrayListOf()
        } while (cursors.isNotEmpty())

        this.blockEntities = blockEntities
        this.cache = cache
        allDirty = false

        return true
    }

    override fun mesh(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        consumer: BlockMeshCollector.Consumer
    ) {
        val mutable = BlockPos.MutableBlockPos()

        cache.collect.forEach { pos ->
            if (isCancelled()) {
                return
            }

            mutable.set(pos.x, pos.y, pos.z)
            val state = level.getBlockState(mutable)
            val offset =
                IVec3((pos.x - this.pos.x).toFloat(), (pos.y - this.pos.y).toFloat(), (pos.z - this.pos.z).toFloat())

            BlockMeshCollector.collectLightFaces(
                manager,
                state,
                level,
                mutable,
                { face ->
                    face.copyWithOffset(offset.x, offset.y, offset.z) // TODO
                },
                consumer
            )
        }
    }
}