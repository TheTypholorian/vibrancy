package net.typho.vibrancy.sky.impl

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.IFramebuffer
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoFramebuffer
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.InterpolationType
import net.typho.big_shot_lib.gl.resource.TextureFormat
import net.typho.big_shot_lib.gl.state.BlendEquation
import net.typho.big_shot_lib.gl.state.ColorMask
import net.typho.big_shot_lib.gl.state.DepthMask
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.sky.SkyLightType
import net.typho.vibrancy.sky.impl.OverworldSkyLight.Section
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object OverworldSkyLightType : SkyLightType<OverworldSkyLightInfo, OverworldSkyLight> {
    val target by lazy {
        val fbo = NeoFramebuffer.TextureBacked(
            Vibrancy.id("sky_shadow_texture"),
            arrayOf(TextureFormat.R32F),
            null,
            1600,
            1600
        )
        (fbo.colorAttachments[0] as ITexture).setInterpolation(InterpolationType.LINEAR)
        fbo
    }
    var worldTransformation: Matrix4f? = null

    override fun infoCodec(level: Level): MapCodec<OverworldSkyLightInfo> {
        return RecordCodecBuilder.mapCodec {
            it.group(
                ExtraCodecs.VECTOR3F
                    .fieldOf("sunColor")
                    .forGetter { info -> info.sunColor },
                ExtraCodecs.VECTOR3F
                    .fieldOf("moonColor")
                    .forGetter { info -> info.moonColor }
            ).apply(it, ::OverworldSkyLightInfo)
        }
    }

    override fun render(
        manager: LightManager,
        light: OverworldSkyLight,
        fbo: IFramebuffer
    ): LightRenderResult {
        val level = manager.getLevel()

        for (pos in manager.dirtyBlocks) {
            if (level.dimension() == pos.dimension()) {
                val chunkPos = ChunkPos(pos.pos)
                light.sections.computeIfAbsent(chunkPos, ::Section).dirty = true
            }
        }

        var sunAngle = level.getSunAngle(manager.getTickDelta(true))
        var x = -sin(sunAngle)
        var y = cos(sunAngle)

        if (y < 0) {
            sunAngle += PI.toFloat()
            x = -x
            y = -y
        }

        val lightDirection = Vector3f(x, y, 0f)
        val worldTransformation = Matrix4f().rotateX(sunAngle)
        this.worldTransformation = worldTransformation

        GlStack().use { stack ->
            target.bind(stack)

            GlStateManager._viewport(0, 0, target.width(), target.height())
            GlStateManager._clearColor(1f, 1f, 1f, 1f)
            GlStateManager._clear(GL_COLOR_BUFFER_BIT, false)

            val shader = NeoShader.get(Vibrancy.id("sky/shadow"))!!

            shader.bind(stack)
            shader.setCommonUniforms(modelViewMat = worldTransformation)

            shader.setSampler("Sampler0", Minecraft.getInstance().textureManager.getTexture(InventoryMenu.BLOCK_ATLAS))

            stack.disable(GlCapability.DEPTH_TEST)
            stack.enable(GlCapability.BLEND)
            stack.set(BlendEquation.MIN)
            stack.set(ColorMask(true, true, true, true))
            stack.set(DepthMask, true)
            stack.disable(GlCapability.CULL_FACE)

            for (section in light.sections.values) {
                if (section.dirty) {
                    section.reload(manager, level.getChunk(section.pos.x, section.pos.z))
                    section.dirty = false
                }

                if (section.any) {
                    section.vbo.bind()
                    section.vbo.draw()
                }
            }

            VertexBuffer.unbind()
        }

        return LightRenderResult(
            numRendered = 1,
            numRaytraced = 1,
            numShadows = 0, // TODO
            numAsyncTasks = 0
        )
    }
}