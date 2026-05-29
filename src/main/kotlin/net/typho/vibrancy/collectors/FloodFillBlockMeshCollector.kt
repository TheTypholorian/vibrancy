package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy.isPointingTowardsInclusive

class FloodFillBlockMeshCollector(
    @JvmField
    val pos: BlockPos,
    @JvmField
    val boundingBox: AbstractRect3<Int>
) : BlockMeshCollector {
    class Cache(
        @JvmField
        var checked: MutableSet<BlockPos> = hashSetOf(),
        @JvmField
        var collect: MutableSet<BlockPos> = hashSetOf()
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
            cache.checked.clear()
            cache.collect.clear()
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
                    cache.checked.add(cursor)
                    cache.collect.add(cursor)
                }

                for (direction in NeoDirection.entries) {
                    mutable.setWithOffset(cursor, direction.mojang)

                    if (direction.isPointingTowardsInclusive(this.pos, cursor)) {// && cache.checked.isInBounds(mutable)) {
                        val immutable = mutable.immutable()

                        if (cache.checked.add(immutable)) {
                            val state = level.getBlockState(mutable)

                            if (consumers.any { it.predicate.shouldCastBlock(level, mutable, state) }) {
                                cache.collect.add(immutable)

                                if (consumers.any { it.predicate.isBlockTransparent(level, mutable, state) }) {
                                    newCursors.add(mutable)
                                }
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

        cache.collect.forEach { pos ->
            if (isCancelled()) {
                return false
            }

            val pos = NeoVec3i(pos) + boundingBox.min
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

        this.blockEntities = blockEntities
        allDirty = false

        return true
    }
}

/*
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy.isPointingTowardsInclusive

private typealias Position = Int

class FloodFillBlockMeshCollector(
    @JvmField
    val pos: BlockPos,
    @JvmField
    val boundingBox: AbstractRect3<Int>
) : BlockMeshCollector {
    class Cache(
        @JvmField
        var checked: MutableSet<Position> = hashSetOf(),
        @JvmField
        var collect: MutableSet<Position> = hashSetOf()
    )

    @JvmField
    val dirty: MutableList<BlockPos> = arrayListOf(pos)
    @JvmField
    var cache: Cache = Cache()
    var blockEntities: MutableSet<BlockPos> = hashSetOf()
        private set
    @JvmField
    var allDirty = true

    init {
        if (boundingBox.size.x > 256 || boundingBox.size.y > 256 || boundingBox.size.z > 256) {
            throw IndexOutOfBoundsException("$boundingBox")
        }
    }

    private val Position.x: Int
        get() = this ushr 16 and 0xFF
    private val Position.y: Int
        get() = this ushr 8 and 0xFF
    private val Position.z: Int
        get() = this and 0xFF

    fun position(x: Int, y: Int, z: Int): Position = positionRelative(x - boundingBox.min.x, y - boundingBox.min.y, z - boundingBox.min.z)

    fun positionRelative(x: Int, y: Int, z: Int): Position = (x shl 16) or (y shl 8) or z

    fun position(pos: BlockPos): Position = position(pos.x, pos.y, pos.z)

    fun positionRelative(pos: BlockPos): Position = positionRelative(pos.x, pos.y, pos.z)

    // TODO when block changed scan blocks, when section changed only collect meshes (don't scan)
    fun markAllDirty() {
        if (!allDirty) {
            allDirty = true
            dirty.clear()
            dirty.add(pos)
            cache.checked.clear()
            cache.collect.clear()
            blockEntities = hashSetOf()
        }
    }

    fun markDirty(pos: BlockPos): Boolean {
                                // TODO
        if (boundingBox.contains(NeoVec3i(pos)) && cache.checked.contains(positionRelative(pos))) {
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
                    val index = positionRelative(cursor)
                    cache.checked.add(index)
                    cache.collect.add(index)
                }

                for (direction in NeoDirection.entries) {
                    mutable.setWithOffset(cursor, direction.mojang)
                    val index = positionRelative(mutable)

                                            // TODO
                    if (boundingBox.contains(NeoVec3i(mutable)) && direction.isPointingTowardsInclusive(this.pos, cursor) && cache.checked.add(index)) {
                        val state = level.getBlockState(mutable)

                        if (consumers.any { it.predicate.shouldCastBlock(level, mutable, state) }) {
                            cache.collect.add(index)

                            if (consumers.any { it.predicate.isBlockTransparent(level, mutable, state) }) {
                                newCursors.add(mutable)
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

        cache.collect.forEach { index ->
            if (isCancelled()) {
                return false
            }

            val pos = NeoVec3i(index.x + boundingBox.min.x, index.y + boundingBox.min.y, index.z + boundingBox.min.z)
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

        this.blockEntities = blockEntities
        allDirty = false

        return true
    }
}
 */