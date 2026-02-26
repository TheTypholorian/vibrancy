package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.shaders.NeoShaderRegistry
import net.typho.big_shot_lib.api.client.opengl.state.CullFace
import net.typho.big_shot_lib.api.client.opengl.state.RenderSettings
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.InterpolationType
import net.typho.big_shot_lib.api.client.opengl.util.OpenGL
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.dynamic_buffers.NormalsDynamicBuffer
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLight
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.shadows.AsyncBlockShadowTexture
import net.typho.vibrancy.shadows.ShadowPredicate
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.ceil

class RayPointLight(
    val color: Vector3f,
    val radius: Float,
    val offset: Vector3f,
    val pos: BlockPos
) : BlockLight<RayPointLightInfo, RayPointLight> {
    val shadows = AsyncBlockShadowTexture(
        { NeoShaderRegistry.get(Vibrancy.id("block/raytraced/shadow_texture"))!! },
        { shader ->
            shader.getUniform("LightPos")?.setValue(getAbsolutePos())
            shader.getUniform("LightRadius")?.setValue(radius)
        },
        Vibrancy.config.blockLights.raytraced.backgroundShadowQuality * 6,
        Vibrancy.config.blockLights.raytraced.backgroundShadowQuality
    )
    val boxBuffer by lazy {
        val mesh = Mesh(
            NeoVertexFormat.POSITION,
            GlShapeType.QUADS,
            BufferUsage.STATIC_DRAW
        )
        val builder = mesh.Builder()
        builder.cube(getBoundingBox())
        builder.end()
        return@lazy mesh
    }
    var shadowsDirty = true

    constructor(info: RayPointLightInfo, state: BlockState, pos: BlockPos) : this(
        info.color.apply(state).mul(info.brightness.apply(state), Vector3f()),
        info.radius.apply(state),
        info.offset.apply(state),
        pos
    )

    override fun getBlockPos() = pos

    override fun getAbsolutePos(): Vector3f {
        return Vector3f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()).add(offset)
    }

    override fun getBoundingBox(): AABB {
        val radius2 = (radius * 2).toDouble()
        val pos = getAbsolutePos()
        return AABB.ofSize(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()), radius2, radius2, radius2)
    }

    override fun rebuildShadows(manager: LightManager) {
        shadows.rebuildAsync(manager, manager.createShadowMesher(this), this)
    }

    override fun resizeShadows(manager: LightManager) {
        shadows.texture.resize(
            Vibrancy.config.blockLights.raytraced.backgroundShadowQuality * 6,
            Vibrancy.config.blockLights.raytraced.backgroundShadowQuality
        )
        shadows.texture.setInterpolation(InterpolationType.LINEAR)
    }

    override fun getShadowBox(): BlockBox {
        val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.toFloat())).toInt()
        return BlockBox.of(
            BlockPos(pos.x - shadowRadius, pos.y - shadowRadius, pos.z - shadowRadius),
            BlockPos(pos.x + shadowRadius, pos.y + shadowRadius, pos.z + shadowRadius)
        )
    }

    override fun getShadowPredicate(): ShadowPredicate {
        return object : ShadowPredicate {
            override fun shouldCastBlock(
                state: BlockState,
                level: Level,
                pos: BlockPos
            ): Boolean {
                return pos != this@RayPointLight.pos && (BlockUtil.INSTANCE.isSolidRender(state, pos, level) || !BlockLightRegistry.has(state.block))
            }

            override fun shouldCastFace(
                face: Direction?,
                state: BlockState,
                level: Level,
                pos: BlockPos
            ): Boolean {
                if (face == null) {
                    return true
                }

                val sidePos = pos.relative(face)

                if (sidePos == this@RayPointLight.pos) {
                    return true
                }

                if (face.step().dot(this@RayPointLight.pos.center.subtract(pos.center).toVector3f()) <= 0) {
                    return false
                }

                val sideState = level.getBlockState(sidePos)

                return !(BlockUtil.INSTANCE.isSolidRender(state, pos, level) && BlockUtil.INSTANCE.isSolidRender(sideState, sidePos, level))
            }

            override fun isInRange(pos: BlockPos): Boolean {
                val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.toFloat())).toInt()
                return pos.distSqr(this@RayPointLight.pos) <= shadowRadius * shadowRadius
            }
        }
    }

    override fun getType() = RayPointLightType

    override fun shouldRaytrace(manager: LightManager) = true

    override fun free(manager: LightManager) {
        shadows.free()
        boxBuffer.free()
    }

    fun render(manager: LightManager, data: RenderEventData, raytrace: Boolean, foreground: Boolean, fbo: GlFramebuffer): LightRenderResult {
        val result = LightRenderResult(
            numRendered = 1,
            numRaytraced = if (raytrace) 1 else 0,
            numForeground = if (foreground) 1 else 0,
            numShadows = if (raytrace) shadows.size else 0,
            numAsyncTasks = if (shadows.isTaskActive()) 1 else 0
        )

        for (pos in manager.dirtyBlocks) {
            if (manager.getLevel().dimension() == pos.dimension && getShadowBox().contains(pos.pos)) {
                shadowsDirty = true
                break
            }
        }

        if (shadowsDirty && raytrace) {
            rebuildShadows(manager)
            shadowsDirty = false
        }

        shadows.checkIfFinished()

        fbo.bind()
        fbo.viewport()

        val highQuality = raytrace && foreground
        var highQualitySettings: RenderSettings? = null

        if (highQuality) {
            fbo.clear(ClearBit.Stencil(0))
            val settings = shadows.renderStencil(
                NeoShaderRegistry.get(Vibrancy.id("block/raytraced/shadow_volume"))!!,
            ) { shader ->
                shader.setCommonUniforms(data)

                shader.getUniform("IProjMat")?.setValue(Matrix4f(data.inverseProjMat))
                shader.getUniform("IModelMat")?.setValue(Matrix4f(data.inverseModelViewMat))

                shader.getUniform("LightPos")?.setValue(getAbsolutePos())
                shader.getUniform("LightRadius")?.setValue(radius)
                shader.getUniform("ScreenSize")?.setValue(fbo.width().toFloat(), fbo.height().toFloat())
                shader.getUniform("CameraPos")?.setValue(data.camera.pos)

                shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture))
                shader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.worldPosFbo.colorAttachments[0] as GlTexture)
            }
            settings.bind()
            highQualitySettings = settings
        }

        val boxShader = NeoShaderRegistry.get(Vibrancy.id("block/raytraced/box"))!!

        boxShader.bind()
        boxShader.setCommonUniforms(data)

        boxShader.getUniform("IProjMat")?.setValue(Matrix4f(data.inverseProjMat))
        boxShader.getUniform("IModelMat")?.setValue(Matrix4f(data.inverseModelViewMat))

        boxShader.getUniform("CameraPos")?.setValue(data.camera.pos)
        boxShader.getUniform("LightPos")?.setValue(getAbsolutePos())
        boxShader.getUniform("LightColor")?.setValue(Vector3f(color).mul(Vibrancy.config.blockLights.raytraced.brightness))
        boxShader.getUniform("LightRadius")?.setValue(radius)
        boxShader.getUniform("ScreenSize")?.setValue(fbo.width().toFloat(), fbo.height().toFloat())

        boxShader.getUniform("ShadowTextureSize")?.setValue(shadows.target.width(), shadows.target.height())
        boxShader.getUniform("ShadowMultiplier")?.setValue(if (highQuality) 1f - Math.clamp((pos.center.toVector3f().distance(data.camera.pos) - (Vibrancy.config.blockLights.raytraced.foregroundDistance * 16 - 8)) / 8f, 0f, 1f) else 0f)
        boxShader.getUniform("VibrancyShadowSampler")?.setSampler(shadows.texture)

        boxShader.getUniform("VibrancyNormalSampler")?.setSampler(NormalsDynamicBuffer.texture)
        boxShader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.worldPosFbo.colorAttachments[0] as GlTexture)

        OpenGL.INSTANCE.cullFace(CullFace.FRONT)

        boxBuffer.draw()

        boxShader.unbind()

        highQualitySettings?.unbind()

        fbo.unbind()

        return result
    }
}