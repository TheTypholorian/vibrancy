package net.typho.vibrancy.collectors

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlasSprite
import net.typho.big_shot_lib.api.client.rendering.util.quad.BasicBakedQuad
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoVertexData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.NeoVec2f
import net.typho.big_shot_lib.api.math.vec.NeoVec3f
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy.isPointingTowardsInclusive

class FloodFillBlockMeshCollector(
    @JvmField
    val pos: BlockPos
) : BlockMeshCollector {
    @JvmField
    val dirty: MutableList<BlockPos> = arrayListOf(pos)
    @JvmField
    val checked: MutableSet<BlockPos> = hashSetOf()
    @JvmField
    val collect: MutableSet<BlockPos> = hashSetOf()
    var blockEntities: MutableSet<BlockPos> = hashSetOf()
        private set

    fun markAllDirty() {
        dirty.clear()
        dirty.add(pos)
        checked.clear()
        collect.clear()
        blockEntities = hashSetOf()
    }

    fun markDirty(pos: BlockPos): Boolean {
        if (checked.contains(pos)) {
            dirty.add(pos)
            return true
        } else {
            return false
        }
    }

    override fun submit(
        manager: LightManager,
        level: Level,
        atlas: NeoAtlas,
        vararg consumers: BlockMeshCollector.Consumer
    ) {
        var cursors = dirty.toMutableList()
        var newCursors = arrayListOf<BlockPos>()

        do {
            while (cursors.isNotEmpty()) {
                val cursor = cursors.removeLast()
                val mutable = BlockPos.MutableBlockPos().set(cursor)

                if (consumers.any { it.predicate.shouldCastBlock(level, mutable, null) }) {
                    checked.add(cursor)
                    collect.add(cursor)
                }

                for (direction in NeoDirection.entries) {
                    mutable.move(direction.mojang)

                    if (direction.isPointingTowardsInclusive(this.pos, cursor) && checked.add(pos)) {
                        val state = level.getBlockState(pos)

                        if (consumers.any { it.predicate.shouldCastBlock(level, mutable, state) }) {
                            collect.add(pos)

                            if (consumers.any { it.predicate.isBlockTransparent(level, mutable, state) }) {
                                newCursors.add(pos)
                            }
                        }
                    }

                    mutable.move(direction.mojang.opposite)
                }
            }

            cursors = newCursors
            newCursors = arrayListOf()
        } while (cursors.isNotEmpty())

        val blockEntities = hashSetOf<BlockPos>()

        collect.sortedBy { it.distSqr(pos) }.forEach { pos ->
            val state = level.getBlockState(pos)
            val mutable = BlockPos.MutableBlockPos().set(pos)

            BlockMeshCollector.collectLightFaces(
                manager,
                state,
                level,
                mutable,
                NeoVec3i(pos.subtract(this.pos)),
                atlas,
                true,
                *consumers
            )

            if (level.getBlockEntity(pos) != null) {
                blockEntities.add(pos)
            }
        }

        this.blockEntities = blockEntities

        /*
        val directions = arrayOf(
            NeoDirection.EAST,
            NeoDirection.WEST,
            NeoDirection.UP,
            NeoDirection.DOWN
        )
        val sprite = atlas.sprites[NeoIdentifier("missingno")]!!

        checked.groupBy({ it.x to it.y }, { it.z }).forEach { (xy, blocks) ->
            var start: Int? = null
            var length = 0
            var last: Int? = null

            fun end() {
                if (start != null) {
                    if (BlockUtil.INSTANCE.shouldRenderFace(level, NeoVec3i(xy.first, xy.second, start!!), NeoDirection.NORTH)) {
                        val pos = NeoVec3i(xy.first, xy.second, start!!)
                        out(
                            LightFace(
                                pos,
                                level.getBlockState(pos.blockPos),
                                NeoDirection.NORTH.createFace(
                                    pos.toFloat(),
                                    1f,
                                    1f,
                                    sprite
                                ),
                                sprite.width,
                                sprite.height
                            )
                        )
                    }

                    if (BlockUtil.INSTANCE.shouldRenderFace(level, NeoVec3i(xy.first, xy.second, start!! + length - 1), NeoDirection.SOUTH)) {
                        val pos = NeoVec3i(xy.first, xy.second, start!! + length - 1)
                        out(
                            LightFace(
                                pos,
                                level.getBlockState(pos.blockPos),
                                NeoDirection.SOUTH.createFace(
                                    pos.toFloat(),
                                    1f,
                                    1f,
                                    sprite
                                ),
                                sprite.width,
                                sprite.height
                            )
                        )
                    }

                    for (dir in directions) {
                        var start = start!!
                        var length = length

                        for (z in start until (start + length)) {
                            if (BlockUtil.INSTANCE.shouldRenderFace(level, NeoVec3i(xy.first, xy.second, z), dir)) {
                                break
                            } else {
                                start++
                                length--
                            }
                        }

                        for (z in (start until (start + length)).reversed()) {
                            if (BlockUtil.INSTANCE.shouldRenderFace(level, NeoVec3i(xy.first, xy.second, z), dir)) {
                                break
                            } else {
                                length--
                            }
                        }

                        if (length > 0) {
                            val pos = NeoVec3i(xy.first, xy.second, start)
                            out(
                                LightFace(
                                    pos,
                                    level.getBlockState(pos.blockPos),
                                    if (dir.axis == NeoDirection.Axis.Y) {
                                        dir.createFace(
                                            pos.toFloat(),
                                            1f,
                                            length.toFloat(),
                                            sprite
                                        )
                                    } else {
                                        dir.createFace(
                                            pos.toFloat(),
                                            length.toFloat(),
                                            1f,
                                            sprite
                                        )
                                    },
                                    if (dir.axis == NeoDirection.Axis.Y) sprite.width else sprite.width * length,
                                    if (dir.axis == NeoDirection.Axis.Y) sprite.height * length else sprite.height
                                )
                            )
                        }
                    }
                }

                start = null
                length = 0
            }

            for (z in blocks.sorted()) {
                if (last?.let { it + 1 != z } == true) {
                    end()
                }

                val pos = NeoVec3i(xy.first, xy.second, z)

                if (BlockUtil.INSTANCE.isSolidRender(level.getBlockState(pos.blockPos), pos, level)) {
                    if (start == null) {
                        start = z
                    }

                    length++
                } else {
                    end()
                    val state = level.getBlockState(pos.blockPos)
                    ShadowMesher.collectLightFaces(
                        manager,
                        state,
                        level,
                        pos,
                        atlas,
                        { predicate.shouldCastFace(it, level, pos, state) },
                        { dir, face -> out(face) }
                    )
                }

                last = z
            }

            end()
        }
         */
    }

    companion object {
        @JvmStatic
        fun NeoDirection.createFace(
            origin: IVec3<Float>,
            width: Float,
            height: Float,
            sprite: NeoAtlasSprite
        ): NeoBakedQuad {
            return BasicBakedQuad(
                when (this) {
                    NeoDirection.NORTH -> arrayOf(
                        NeoVertexData(
                            origin,
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(width, 0f, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(width, height, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v1),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(0f, height, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v1),
                            normal = toFloat()
                        )
                    )

                    NeoDirection.SOUTH -> arrayOf(
                        NeoVertexData(
                            origin + NeoVec3f(width, 0f, 1f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(0f, 0f, 1f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(0f, height, 1f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v1),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(width, height, 1f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v1),
                            normal = toFloat()
                        )
                    )

                    NeoDirection.WEST -> arrayOf(
                        NeoVertexData(
                            origin + NeoVec3f(0f, 0f, width),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin,
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(0f, height, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v1),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(0f, height, width),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v1),
                            normal = toFloat()
                        )
                    )

                    NeoDirection.EAST -> arrayOf(
                        NeoVertexData(
                            origin + NeoVec3f(1f, 0f, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(1f, 0f, width),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(1f, height, width),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v1),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(1f, height, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v1),
                            normal = toFloat()
                        )
                    )

                    NeoDirection.DOWN -> arrayOf(
                        NeoVertexData(
                            origin + NeoVec3f(0f, 0f, height),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(width, 0f, height),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(width, 0f, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v1),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin,
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v1),
                            normal = toFloat()
                        )
                    )

                    NeoDirection.UP -> arrayOf(
                        NeoVertexData(
                            origin + NeoVec3f(0f, 1f, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(width, 1f, 0f),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v0),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(width, 1f, height),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u1, sprite.v1),
                            normal = toFloat()
                        ),
                        NeoVertexData(
                            origin + NeoVec3f(0f, 1f, height),
                            NeoColor.FULL_ON,
                            NeoVec2f(sprite.u0, sprite.v1),
                            normal = toFloat()
                        )
                    )
                },
                null,
                this,
                null,
                false
            )
        }
    }
}