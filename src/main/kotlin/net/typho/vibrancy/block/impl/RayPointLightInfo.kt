package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendEquation
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendingFactor
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlBlendShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoMultiBufferSource
import net.typho.big_shot_lib.api.client.rendering.util.NeoRenderSettings
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.math.vec.IVec2
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.NeoVec3f
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.util.StateFunction
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER

data class RayPointLightInfo(
    @JvmField
    val color: StateFunction<IVec3<Float>>,
    @JvmField
    val radius: StateFunction<Float>,
    @JvmField
    val brightness: StateFunction<Float>,
    @JvmField
    val offset: StateFunction<IVec3<Float>>,
    override val enabled: StateFunction<Boolean>
) : BlockLightInfo {
    override val type = RayPointLightType

    companion object {
        @JvmStatic
        fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<RayPointLightInfo> = RecordCodecBuilder.mapCodec {
            it.group(
                StateFunction.codec(IVec3.FLOAT_CODEC, stateDefinition)
                    .fieldOf("color")
                    .forGetter { info -> info.color },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("radius")
                    .forGetter { info -> info.radius },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness },
                StateFunction.codec(IVec3.FLOAT_CODEC, stateDefinition)
                    .optionalFieldOf("offset", StateFunction(NeoVec3f(0.5f, 0.5f, 0.5f)))
                    .forGetter { info -> info.offset },
                StateFunction.codec(Codec.BOOL, stateDefinition)
                    .optionalFieldOf("enabled", StateFunction(true))
                    .forGetter { info -> info.enabled }
            ).apply(it, ::RayPointLightInfo)
        }
    }
}