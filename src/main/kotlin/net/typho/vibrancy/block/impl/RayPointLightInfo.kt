package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
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
import net.typho.vibrancy.shadows.RaytracedGuiGraphics
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

    override fun renderInventoryLight(
        absolutePos: IVec2<Int>,
        shadowPos: IVec3<Float>,
        width: Int,
        height: Int,
        stack: ItemStack,
        block: BlockState,
        quads: Map<NeoRenderSettings, List<NeoBakedQuad>>,
        blitQuads: Map<GlTexture2D, List<NeoBakedQuad>>,
        buffers: NeoMultiBufferSource
    ): Boolean {
        fun render(texture: GlTexture2D, quads: List<NeoBakedQuad>) {
            val settings = NeoRenderSettings.Basic(
                Vibrancy.id("ray_point_inventory"),
                LightMesh.INVENTORY_VERTEX_FORMAT,
                drawState = LightMesh.drawState(
                    NeoAtlas.blocks,
                    Vibrancy.id("block/raytraced/inventory"),
                    false
                ) {
                    setUniform("ModelViewMat") { set(RenderSystem.getModelViewMatrix()) }
                    setUniform("ProjMat") { set(RenderSystem.getProjectionMatrix()) }

                    setUniform("LightCoords") { setIntVec(absolutePos) }
                    setUniform("LightPos") { setFloatVec(shadowPos) }
                    setUniform("LightColor") { setFloatVec(color(block)) }
                    setUniform("LightRadius") { set(radius(block) * VibrancyConfig.inventoryLightScale * Minecraft.getInstance().window.guiScale.toFloat()) }
                    setUniform("LightBrightness") { set(VibrancyConfig.inventoryLightBrightness) }

                    setUniform("ScreenSize") { set(width, height) }

                    setUniform("Scale") { set(Minecraft.getInstance().window.guiScale.toFloat()) }

                    setTexture(0, GlTextureBinding.FromInstance(texture, GlTextureTarget.TEXTURE_2D))
                    setTexture(1, GlTextureBinding.FromInstance(NeoAtlas.blocks, GlTextureTarget.TEXTURE_2D))

                    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, RaytracedGuiGraphics.shadowBuffer.glId)
                }
            )
            val buffer = buffers.getBuffer(settings)

            for (quad in quads) {
                if (quad.v0.textureUV != null) {
                    quad.put(buffer)
                }
            }
        }

        for (entry in quads) {
            if (entry.key.format.contains(NeoVertexFormat.Element.TEXTURE_UV)) {
                entry.key.drawState.shader.textures.getOrNull(0)?.let { texture ->
                    render(texture.texture, entry.value)
                }
            }
        }

        for (entry in blitQuads) {
            render(entry.key, entry.value)
        }

        return true
    }
}