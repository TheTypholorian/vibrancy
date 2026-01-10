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
        internal val MAP = HashMap<Block, BlockLightInfo>()

        @JvmStatic
        fun get(block: Block): BlockLightInfo? = MAP.get(block)

        @JvmStatic
        fun has(block: Block): Boolean = MAP.containsKey(block)
    }
}