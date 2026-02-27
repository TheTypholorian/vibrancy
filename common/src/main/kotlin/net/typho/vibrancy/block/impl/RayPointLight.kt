package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.InterpolationType
import net.typho.big_shot_lib.api.client.opengl.util.MeshUtil
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
import org.joml.Vector3f
import kotlin.math.ceil

class RayPointLight(
    val color: Vector3f,
    val radius: Float,
    val offset: Vector3f,
    val pos: BlockPos
) : BlockLight<RayPointLightInfo, RayPointLight> {
    companion object {
        @JvmStatic
        fun stencilCullSettings(fbo: GlFramebuffer, light: RayPointLight) = RenderSettings(
            Vibrancy.id("block/raytraced/stencil_cull"),
            listOf(
                FramebufferShard(
                    { fbo },
                    true,
                    ClearBit.Stencil(0)
                ),
                StencilShard(
                    true,
                    StencilFunc(ComparisonFunc.ALWAYS, 1, 1),
                    1,
                    StencilOp(IntAction.ZERO, IntAction.ZERO, IntAction.REPLACE)
                ),
                ShaderShard(
                    Vibrancy.id("block/raytraced/stencil_cull")
                ) { shader ->
                    shader.getUniform("LightPos")?.setValue(light.getAbsolutePos())
                    shader.getUniform("LightRadius")?.setValue(light.radius)

                    shader.getUniform("VibrancyNormalSampler")?.setSampler(NormalsDynamicBuffer.texture)
                    shader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.worldPosFbo.colorAttachments[0] as GlTexture)
                }
            )
        )

        @JvmStatic
        fun shadowVolumeSettings(fbo: GlFramebuffer, light: RayPointLight, data: RenderEventData) = RenderSettings(
            Vibrancy.id("block/raytraced/shadow_volume"),
            listOf(
                FramebufferShard(
                    { fbo },
                    true
                ),
                ShaderShard(
                    Vibrancy.id("block/raytraced/shadow_volume")
                ) { shader ->
                    shader.setCommonUniforms(data)

                    shader.getUniform("LightPos")?.setValue(light.getAbsolutePos())
                    shader.getUniform("LightRadius")?.setValue(light.radius)
                    shader.getUniform("ScreenSize")?.setValue(fbo.width.toFloat(), fbo.height.toFloat())
                    shader.getUniform("CameraPos")?.setValue(data.camera.pos)

                    shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture))
                    shader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.worldPosFbo.colorAttachments[0] as GlTexture)
                }
            )
        )

        @JvmStatic
        fun boxSettings(fbo: GlFramebuffer, light: RayPointLight, data: RenderEventData, highQuality: Boolean) =
            RenderSettings(
                Vibrancy.id("block/raytraced/box"),
                listOf(
                    FramebufferShard(
                        { fbo },
                        true
                    ),
                    ShaderShard(
                        Vibrancy.id("block/raytraced/box")
                    ) { shader ->
                        shader.setCommonUniforms(data)

                        shader.getUniform("CameraPos")?.setValue(data.camera.pos)
                        shader.getUniform("LightPos")?.setValue(light.getAbsolutePos())
                        shader.getUniform("LightColor")?.setValue(Vector3f(light.color).mul(Vibrancy.config.blockLights.raytraced.brightness))
                        shader.getUniform("LightRadius")?.setValue(light.radius)
                        shader.getUniform("ScreenSize")?.setValue(fbo.width.toFloat(), fbo.height.toFloat())

                        shader.getUniform("ShadowTextureSize")?.setValue(light.shadows.target.width, light.shadows.target.height)
                        shader.getUniform("ShadowMultiplier")?.setValue(
                            if (highQuality)
                                1f - Math.clamp((light.pos.center.toVector3f().distance(data.camera.pos) - (Vibrancy.config.blockLights.raytraced.foregroundDistance * 16 - 8)) / 8f, 0f, 1f)
                            else
                                0f
                        )
                        shader.getUniform("VibrancyShadowSampler")?.setSampler(light.shadows.texture)

                        shader.getUniform("VibrancyNormalSampler")?.setSampler(NormalsDynamicBuffer.texture)
                        shader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.worldPosFbo.colorAttachments[0] as GlTexture)
                    },
                    CullShard(
                        true,
                        CullFace.FRONT
                    )
                )
            )

        @JvmStatic
        fun shadowTextureShaderShard(light: RayPointLight) = ShaderShard(
            Vibrancy.id("block/raytraced/shadow_texture")
        ) { shader ->
            shader.getUniform("LightPos")?.setValue(light.getAbsolutePos())
            shader.getUniform("LightRadius")?.setValue(light.radius)
            shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture))
        }
    }

    val shadows = AsyncBlockShadowTexture(
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

        shadows.checkIfFinished { shadowTextureShaderShard(this) }

        fbo.bind()
        fbo.viewport()

        val highQuality = raytrace && foreground
        var highQualitySettings: RenderSettings? = null

        if (highQuality) {
            val stencilCullSettings = stencilCullSettings(fbo, this)

            stencilCullSettings.bind()
            MeshUtil.SCREEN_MESH.draw()
            stencilCullSettings.unbind()

            val shadowVolumeSettings = shadowVolumeSettings(fbo, this, data)

            shadowVolumeSettings.bind()
            highQualitySettings = shadows.renderStencil().also { it.bind() }
            shadowVolumeSettings.unbind()
        }

        val boxSettings = boxSettings(fbo, this, data, highQuality)

        boxSettings.bind()
        boxBuffer.draw()
        boxSettings.unbind()

        highQualitySettings?.unbind()

        fbo.unbind()

        return result
    }
}