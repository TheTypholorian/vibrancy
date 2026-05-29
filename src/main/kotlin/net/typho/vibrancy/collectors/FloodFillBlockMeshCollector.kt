package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoVertexData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy.isPointingTowardsInclusive
import net.typho.vibrancy.util.Offset3DArray

class FloodFillBlockMeshCollector(
    @JvmField
    val pos: BlockPos,
    @JvmField
    val boundingBox: AbstractRect3<Int>
) : BlockMeshCollector {
    inner class Cache(
        @JvmField
        var checked: Offset3DArray<Boolean> = Offset3DArray(boundingBox, false),
        @JvmField
        var collect: Offset3DArray<Boolean> = Offset3DArray(boundingBox, false)
    )

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
            cache.checked.fill(false) // TODO
            cache.collect.fill(false)
            blockEntities = hashSetOf()
        }
    }

    fun markDirty(pos: BlockPos): Boolean {
        if (cache.checked.isInBounds(pos) && cache.checked[pos]) {
            dirty.add(pos)
            return true
        } else {
            return false
        }
    }

    override fun submit(
        isCancelled: () -> Boolean,
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        vararg consumers: BlockMeshCollector.Consumer
    ): Boolean {
        var cursors = dirty.toMutableList()
        var newCursors = arrayListOf<BlockPos>()
        val mutable = BlockPos.MutableBlockPos()

        do {
            while (cursors.isNotEmpty()) {
                if (isCancelled()) {
                    return false
                }

                val cursor = cursors.removeLast()
                mutable.set(cursor)

                if (consumers.any { it.predicate.shouldCastBlock(level, mutable, null) }) {
                    cache.checked[cursor] = true
                    cache.collect[cursor] = true
                }

                for (direction in NeoDirection.entries) {
                    val pos = cursor.relative(direction.mojang)
                    mutable.setWithOffset(cursor, direction.mojang)

                    if (cache.checked.isInBounds(pos) && direction.isPointingTowardsInclusive(this.pos, cursor) && !cache.checked.getAndSet(pos, true)) {
                        val state = level.getBlockState(pos)

                        if (consumers.any { it.predicate.shouldCastBlock(level, mutable, state) }) {
                            cache.collect[pos] = true

                            if (consumers.any { it.predicate.isBlockTransparent(level, mutable, state) }) {
                                newCursors.add(pos)
                            }
                        }
                    }
                }
            }

            cursors = newCursors
            newCursors = arrayListOf()
        } while (cursors.isNotEmpty())

        val blockEntities = hashSetOf<BlockPos>()
        val blockPos = BlockPos.MutableBlockPos()

        cache.collect.forEach { (pos, value) ->
            if (isCancelled()) {
                return false
            }

            if (value) {
                val pos = pos + boundingBox.min
                blockPos.set(pos.x, pos.y, pos.z)
                val state = level.getBlockState(blockPos)
                val offset = pos.minus(this.pos.x, this.pos.y, this.pos.z).toFloat()

                BlockMeshCollector.collectLightFaces(
                    manager,
                    state,
                    level,
                    blockPos,
                    { face ->
                        face.copyWithOffset(offset.x, offset.y, offset.z) // TODO
                    },
                    *consumers
                )

                if (level.getBlockEntity(blockPos) != null) {
                    blockEntities.add(blockPos.immutable())
                }
            }
        }

        this.blockEntities = blockEntities
        allDirty = false

        return true
    }
}