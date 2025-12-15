package net.typho.vibrancy.light

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.util.BlockStateFunction
import net.typho.vibrancy.util.getKey
import org.joml.Vector3f
import java.awt.Color
import java.util.*

data class DynamicLightInfo(
    val color: Optional<BlockStateFunction<Color>>,
    val radius: Optional<BlockStateFunction<Float>>,
    val brightness: Optional<BlockStateFunction<Float>>,
    val offset: Optional<BlockStateFunction<Vector3f>>
) {
    constructor() : this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())

    fun copy(block: Block): DynamicLightInfo {
        val key = block.getKey()
        return DynamicLightInfo(
            color.or { MAP[key]!!.color },
            radius.or { MAP[key]!!.radius },
            brightness.or { MAP[key]!!.brightness },
            offset.or { MAP[key]!!.offset }
        )
    }

    fun addBlockLight(pos: BlockPos, state: BlockState) {
        val old = BlockLight.LIGHTS[pos]

        if (old == null) {
            BlockLight.LIGHTS.put(pos, BlockLight(pos, this, state))
        } else {
            old.set(this, state)
        }
    }

    companion object {
        val MAP = HashMap<ResourceKey<Block>, DynamicLightInfo>()

        fun put(block: Block, info: DynamicLightInfo) {
            MAP.put(block.getKey(), info)
        }

        init {
            put(Blocks.TORCH, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(1f, 1f, 0.59f))),
                Optional.empty(),
                Optional.of(BlockStateFunction(0.5f)),
                Optional.of(BlockStateFunction(Vector3f(0.5f, 0.5625f, 0.5f)))
            ))
            put(Blocks.WALL_TORCH, DynamicLightInfo(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(
                    BlockStateFunction(
                        Vector3f(0.5f, 0.5625f, 0.5f),
                        BlockStateFunction.Entry("facing", Direction.EAST, Vector3f(0.25f, 0.8125f, 0.5f)),
                        BlockStateFunction.Entry("facing", Direction.WEST, Vector3f(0.75f, 0.8125f, 0.5f)),
                        BlockStateFunction.Entry("facing", Direction.SOUTH, Vector3f(0.5f, 0.8125f, 0.25f)),
                        BlockStateFunction.Entry("facing", Direction.NORTH, Vector3f(0.5f, 0.8125f, 0.75f))
                    )
                )
            ).copy(Blocks.TORCH))
            put(Blocks.LANTERN, DynamicLightInfo().copy(Blocks.TORCH)) // TODO adjust offset for lantern state
            put(Blocks.CAMPFIRE, DynamicLightInfo(
                Optional.empty(),
                Optional.empty(),
                Optional.of(
                    BlockStateFunction(
                        0f,
                        BlockStateFunction.Entry("lit", true, 0.6f)
                    )
                ),
                Optional.empty()
            ).copy(Blocks.TORCH))
            put(Blocks.SOUL_TORCH, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(0.48f, 1f, 1f))),
                Optional.empty(),
                Optional.of(BlockStateFunction(0.3f)),
                Optional.empty()
            ).copy(Blocks.TORCH))
            put(Blocks.SOUL_WALL_TORCH, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(0.48f, 1f, 1f))),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            ).copy(Blocks.WALL_TORCH))
            put(Blocks.SOUL_LANTERN, DynamicLightInfo().copy(Blocks.SOUL_TORCH))
            put(Blocks.SOUL_CAMPFIRE, DynamicLightInfo(
                Optional.empty(),
                Optional.empty(),
                Optional.of(
                    BlockStateFunction(
                        0f,
                        BlockStateFunction.Entry("lit", true, 0.4f)
                    )
                ),
                Optional.empty()
            ).copy(Blocks.SOUL_TORCH))
            put(Blocks.END_ROD, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(0.96f, 0.88f, 0.8f))),
                Optional.empty(),
                Optional.of(BlockStateFunction(0.4f)),
                Optional.empty()
            ))
            put(Blocks.COPPER_BULB, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(1f, 0.86f, 0.6f))),
                Optional.empty(),
                Optional.of(
                    BlockStateFunction(
                        0f,
                        BlockStateFunction.Entry("lit", true, 0.6f)
                    )
                ),
                Optional.empty()
            ))
            put(Blocks.EXPOSED_COPPER_BULB, DynamicLightInfo(
                Optional.empty(),
                Optional.empty(),
                Optional.of(
                    BlockStateFunction(
                        0f,
                        BlockStateFunction.Entry("lit", true, 0.5f)
                    )
                ),
                Optional.empty()
            ).copy(Blocks.COPPER_BULB))
            put(Blocks.WEATHERED_COPPER_BULB, DynamicLightInfo(
                Optional.empty(),
                Optional.empty(),
                Optional.of(
                    BlockStateFunction(
                        0f,
                        BlockStateFunction.Entry("lit", true, 0.4f)
                    )
                ),
                Optional.empty()
            ).copy(Blocks.COPPER_BULB))
            put(Blocks.OXIDIZED_COPPER_BULB, DynamicLightInfo(
                Optional.empty(),
                Optional.empty(),
                Optional.of(
                    BlockStateFunction(
                        0f,
                        BlockStateFunction.Entry("lit", true, 0.4f)
                    )
                ),
                Optional.empty()
            ).copy(Blocks.COPPER_BULB))
            put(Blocks.WAXED_COPPER_BULB, DynamicLightInfo().copy(Blocks.COPPER_BULB))
            put(Blocks.WAXED_EXPOSED_COPPER_BULB, DynamicLightInfo().copy(Blocks.EXPOSED_COPPER_BULB))
            put(Blocks.WAXED_WEATHERED_COPPER_BULB, DynamicLightInfo().copy(Blocks.WEATHERED_COPPER_BULB))
            put(Blocks.WAXED_OXIDIZED_COPPER_BULB, DynamicLightInfo().copy(Blocks.OXIDIZED_COPPER_BULB))
            put(Blocks.REDSTONE_LAMP, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(0.94f, 0.74f, 0.45f))),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            ).copy(Blocks.COPPER_BULB))
            put(Blocks.BEACON, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(1f, 1f, 1f))),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            ))
            put(Blocks.SEA_LANTERN, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(0.79f, 0.89f, 0.86f))),
                Optional.empty(),
                Optional.of(BlockStateFunction(0.3f)),
                Optional.empty()
            ))
            put(Blocks.CONDUIT, DynamicLightInfo().copy(Blocks.SEA_LANTERN))
            put(Blocks.OCHRE_FROGLIGHT, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(0.98f, 0.93f, 0.69f))),
                Optional.empty(),
                Optional.of(BlockStateFunction(0.5f)),
                Optional.empty()
            ))
            put(Blocks.VERDANT_FROGLIGHT, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(0.84f, 0.93f, 0.69f))),
                Optional.empty(),
                Optional.of(BlockStateFunction(0.5f)),
                Optional.empty()
            ))
            put(Blocks.PEARLESCENT_FROGLIGHT, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(0.93f, 0.9f, 0.88f))),
                Optional.empty(),
                Optional.of(BlockStateFunction(0.5f)),
                Optional.empty()
            ))
            put(Blocks.JACK_O_LANTERN, DynamicLightInfo(
                Optional.of(BlockStateFunction(Color(1f, 1f, 0.63f))),
                Optional.empty(),
                Optional.of(BlockStateFunction(0.3f)),
                Optional.empty()
            ))
        }
    }
}