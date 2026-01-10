package net.typho.vibrancy.light

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.util.BlockStateFunction
import org.joml.Vector3f

data class BlockLightInfo(
    val color: BlockStateFunction<Vector3f>,
    val radius: BlockStateFunction<Float>,
    val brightness: BlockStateFunction<Float>,
    val offset: BlockStateFunction<Vector3f>,
    val enabled: BlockStateFunction<Boolean>
) {
    fun addBlockLight(pos: BlockPos, state: BlockState) {
        val old = BlockLight.LIGHTS[pos]

        if (old == null) {
            BlockLight.LIGHTS.put(pos, BlockLight(pos, this, state))
        } else {
            old.set(this, state)
        }
    }

    companion object {
        @JvmStatic
        fun codec(stateDefinition: StateDefinition<Block, BlockState>): MapCodec<BlockLightInfo> {
            return RecordCodecBuilder.mapCodec {
                it.group(
                    BlockStateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                        .fieldOf("color")
                        .forGetter { info -> info.color },
                    BlockStateFunction.codec(Codec.FLOAT, stateDefinition)
                        .fieldOf("radius")
                        .forGetter { info -> info.radius },
                    BlockStateFunction.codec(Codec.FLOAT, stateDefinition)
                        .fieldOf("brightness")
                        .forGetter { info -> info.brightness },
                    BlockStateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                        .fieldOf("offset")
                        .forGetter { info -> info.offset },
                    BlockStateFunction.codec(Codec.BOOL, stateDefinition)
                        .optionalFieldOf("enabled", BlockStateFunction(true))
                        .forGetter { info -> info.enabled }
                ).apply(it, ::BlockLightInfo)
            }
        }

        @JvmField
        val MAP = HashMap<Block, BlockLightInfo>()

        init {
            /*
            put(Blocks.TORCH, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 1f, 0.59f)),
                brightness = BlockStateFunction(0.5f),
                offset = BlockStateFunction(Vector3f(0.5f, 0.5625f, 0.5f))
            ))
            put(Blocks.WALL_TORCH, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 1f, 0.59f)),
                brightness = BlockStateFunction(0.5f),
                offset = BlockStateFunction(
                    Vector3f(0.5f, 0.5625f, 0.5f),
                    BlockStateFunction.Entry("facing", Direction.EAST, Vector3f(0.25f, 0.8125f, 0.5f)),
                    BlockStateFunction.Entry("facing", Direction.WEST, Vector3f(0.75f, 0.8125f, 0.5f)),
                    BlockStateFunction.Entry("facing", Direction.SOUTH, Vector3f(0.5f, 0.8125f, 0.25f)),
                    BlockStateFunction.Entry("facing", Direction.NORTH, Vector3f(0.5f, 0.8125f, 0.75f))
                )
            ))
            put(Blocks.LANTERN, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 1f, 0.59f)),
                brightness = BlockStateFunction(0.5f),
                offset = BlockStateFunction(
                    Vector3f(0.5f, 0.1875f, 0.5f),
                    BlockStateFunction.Entry("hanging", true, Vector3f(0.5f, 0.25f, 0.5f))
                )
            ))
            put(Blocks.CAMPFIRE, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 1f, 0.59f)),
                brightness = BlockStateFunction(
                    0f,
                    BlockStateFunction.Entry("lit", true, 0.6f)
                ),
                offset = BlockStateFunction(Vector3f(0.5f, 0.5625f, 0.5f))
            ))
            put(Blocks.SOUL_TORCH, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.48f, 1f, 1f)),
                brightness = BlockStateFunction(0.3f),
                offset = BlockStateFunction(Vector3f(0.5f, 0.5625f, 0.5f))
            ))
            put(Blocks.SOUL_WALL_TORCH, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.48f, 1f, 1f)),
                brightness = BlockStateFunction(0.5f),
                offset = BlockStateFunction(
                    Vector3f(0.5f, 0.5625f, 0.5f),
                    BlockStateFunction.Entry("facing", Direction.EAST, Vector3f(0.25f, 0.8125f, 0.5f)),
                    BlockStateFunction.Entry("facing", Direction.WEST, Vector3f(0.75f, 0.8125f, 0.5f)),
                    BlockStateFunction.Entry("facing", Direction.SOUTH, Vector3f(0.5f, 0.8125f, 0.25f)),
                    BlockStateFunction.Entry("facing", Direction.NORTH, Vector3f(0.5f, 0.8125f, 0.75f))
                )
            ))
            put(Blocks.SOUL_LANTERN, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.48f, 1f, 1f)),
                brightness = BlockStateFunction(0.3f),
                offset = BlockStateFunction(
                    Vector3f(0.5f, 0.1875f, 0.5f),
                    BlockStateFunction.Entry("hanging", true, Vector3f(0.5f, 0.25f, 0.5f))
                )
            ))
            put(Blocks.SOUL_CAMPFIRE, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.48f, 1f, 1f)),
                brightness = BlockStateFunction(
                    0f,
                    BlockStateFunction.Entry("lit", true, 0.4f)
                ),
                offset = BlockStateFunction(Vector3f(0.5f, 0.5625f, 0.5f))
            ))
            put(Blocks.END_ROD, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.96f, 0.88f, 0.8f)),
                brightness = BlockStateFunction(0.4f)
            ))
            put(Blocks.COPPER_BULB, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 0.86f, 0.6f)),
                brightness = BlockStateFunction(
                    0f,
                    BlockStateFunction.Entry("lit", true, 0.6f)
                )
            ))
            put(Blocks.EXPOSED_COPPER_BULB, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 0.86f, 0.6f)),
                brightness = BlockStateFunction(
                    0f,
                    BlockStateFunction.Entry("lit", true, 0.5f)
                )
            ))
            put(Blocks.WEATHERED_COPPER_BULB, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 0.86f, 0.6f)),
                brightness = BlockStateFunction(
                    0f,
                    BlockStateFunction.Entry("lit", true, 0.4f)
                )
            ))
            put(Blocks.OXIDIZED_COPPER_BULB, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 0.86f, 0.6f)),
                brightness = BlockStateFunction(
                    0f,
                    BlockStateFunction.Entry("lit", true, 0.4f)
                )
            ))
            put(Blocks.WAXED_COPPER_BULB, get(Blocks.COPPER_BULB)!!)
            put(Blocks.WAXED_EXPOSED_COPPER_BULB, get(Blocks.EXPOSED_COPPER_BULB)!!)
            put(Blocks.WAXED_WEATHERED_COPPER_BULB, get(Blocks.WEATHERED_COPPER_BULB)!!)
            put(Blocks.WAXED_OXIDIZED_COPPER_BULB, get(Blocks.OXIDIZED_COPPER_BULB)!!)
            put(Blocks.REDSTONE_LAMP, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.94f, 0.74f, 0.45f)),
                brightness = BlockStateFunction(
                    0f,
                    BlockStateFunction.Entry("lit", true, 0.6f)
                )
            ))
            put(Blocks.BEACON, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 1f, 1f))
            ))
            put(Blocks.SEA_LANTERN, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.79f, 0.89f, 0.86f)),
                brightness = BlockStateFunction(0.3f)
            ))
            put(Blocks.CONDUIT, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.79f, 0.89f, 0.86f)),
                brightness = BlockStateFunction(0.3f)
            ))
            put(Blocks.OCHRE_FROGLIGHT, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.98f, 0.93f, 0.69f)),
                brightness = BlockStateFunction(0.5f)
            ))
            put(Blocks.VERDANT_FROGLIGHT, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.84f, 0.93f, 0.69f)),
                brightness = BlockStateFunction(0.5f)
            ))
            put(Blocks.PEARLESCENT_FROGLIGHT, BlockLightInfo(
                color = BlockStateFunction(Vector3f(0.93f, 0.9f, 0.88f)),
                brightness = BlockStateFunction(0.5f)
            ))
            put(Blocks.JACK_O_LANTERN, BlockLightInfo(
                color = BlockStateFunction(Vector3f(1f, 1f, 0.63f)),
                brightness = BlockStateFunction(0.3f)
            ))
             */
        }
    }
}