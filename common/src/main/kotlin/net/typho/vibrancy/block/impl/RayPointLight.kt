package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.BigShotLib.cube
import net.typho.big_shot_lib.api.IFramebuffer
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyDynamicBuffers
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
        { NeoShader.get(Vibrancy.id("block/raytraced/shadow"))!! },
        { shader ->
            shader.getUniform("LightPos")?.set(getAbsolutePos())
            shader.getUniform("LightRadius")?.set(radius)
        },
        Vibrancy.config.blockLights.raytraced.shadowTextureSize.get() * 6,
        Vibrancy.config.blockLights.raytraced.shadowTextureSize.get()
    )
    val boxBuffer by lazy {
        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.cube(getBoundingBox())

        val vbo = VertexBuffer(VertexBuffer.Usage.STATIC)

        vbo.bind()
        vbo.upload(builder.buildOrThrow())
        VertexBuffer.unbind()

        return@lazy vbo
    }
    var shadowsDirty = true

    constructor(info: RayPointLightInfo, state: StateHolder<*, *>, pos: BlockPos) : this(
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
        return AABB.ofSize(Vec3(getAbsolutePos()), radius2, radius2, radius2)
    }

    override fun rebuildShadows(manager: LightManager) {
        shadows.rebuildAsync(manager, manager.createShadowMesher(this), this)
    }

    override fun getShadowBox(): BlockBox {
        val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.get().toFloat())).toInt()
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
                return pos != this@RayPointLight.pos && (state.isSolidRender(level, pos) || !BlockLightRegistry.has(state.block))
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

                if (
                    Vec3.atLowerCornerOf(face.normal).toVector3f()
                        .dot(this@RayPointLight.pos.center.subtract(pos.center).toVector3f()) <= 0
                ) {
                    return false
                }

                val sideState = level.getBlockState(sidePos)

                return !(state.isSolidRender(level, pos) && sideState.isSolidRender(level, pos))
            }

            override fun isInRange(pos: BlockPos): Boolean {
                val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.get().toFloat())).toInt()
                return pos.distSqr(this@RayPointLight.pos) <= shadowRadius * shadowRadius
            }
        }
    }

    override fun getType() = RayPointLightType

    override fun shouldRender(manager: LightManager): Boolean = true

    override fun shouldRaytrace(manager: LightManager) = true

    override fun free(manager: LightManager) {
        shadows.free()
        boxBuffer.close()
    }

    fun render(manager: LightManager, raytrace: Boolean, stack: GlStack, fbo: IFramebuffer): LightRenderResult {
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

        if (shadows.checkIfFinished()) {
            getType().initState(stack)
        }

        val result = LightRenderResult(
            numRendered = 1,
            numRaytraced = if (raytrace) 1 else 0,
            numShadows = if (raytrace) shadows.size else 0,
            numAsyncTasks = if (shadows.isTaskActive()) 1 else 0
        )

        fbo.bind(stack)
        GlStateManager._viewport(0, 0, fbo.width(), fbo.height())

        val boxShader = NeoShader.get(Vibrancy.id("block/raytraced/box"))!!

        boxShader.bind(stack)
        boxShader.setCommonUniforms(modelViewMat = manager.getViewMatrix())

        boxShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
        boxShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

        boxShader.getUniform("LightPos")?.set(getAbsolutePos())
        boxShader.getUniform("LightColor")?.set(Vector3f(color).mul(Vibrancy.config.blockLights.raytraced.brightness.get()))
        boxShader.getUniform("LightRadius")?.set(radius)
        boxShader.getUniform("CameraPos")?.set(Vibrancy.camera)
        boxShader.getUniform("ScreenSize")?.set(fbo.width().toFloat(), fbo.height().toFloat())

        boxShader.getUniform("ShadowTextureSize")?.set(shadows.target.width(), shadows.target.height())

        boxShader.getUniform("SampleShadows")?.set(if (raytrace) 1 else 0)

        boxShader.setSampler("VibrancyShadowSampler", shadows.target.colorAttachments[0] as ITexture)
        boxShader.setSampler("VibrancyNormalSampler", VibrancyDynamicBuffers.normalsTexture!!)
        boxShader.setSampler("VibrancyWorldPosSampler", Vibrancy.WORLD_POS_FBO.colorAttachments[0] as ITexture)

        stack.enable(GlCapability.CULL_FACE)

        boxBuffer.bind()
        boxBuffer.draw()
        VertexBuffer.unbind()

        return result
    }
}