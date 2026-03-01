package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.VertexSorting
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
import net.typho.big_shot_lib.api.client.opengl.util.MeshUtil
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.shadows.AsyncBlockShadowTexture
import net.typho.vibrancy.shadows.ShadowPredicate
import net.typho.vibrancy.shadows.ShadowTexture
import net.typho.vibrancy.util.PointLight
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.glPolygonOffset
import org.lwjgl.system.NativeResource
import kotlin.math.ceil

open class RayPointLight(
    @JvmField
    val color: Vector3f,
    @JvmField
    val radius: Float,
    @JvmField
    val offset: Vector3f,
    override val blockPos: BlockPos
) : PointLight, NativeResource {
    companion object {
        @JvmStatic
        fun meshSettings(fbo: GlFramebuffer, light: RayPointLight, data: RenderEventData) =
            RenderSettings(
                Vibrancy.id("block/raytraced/mesh"),
                listOf(
                    FramebufferShard(
                        { fbo },
                        true
                    ),
                    CullShard(
                        true,
                        CullFace.BACK
                    ),
                    DepthMaskShard(
                        false
                    ),
                    DepthTestShard(
                        true,
                        ComparisonFunc.LEQUAL
                    ),
                    ShaderShard(
                        Vibrancy.id("block/raytraced/mesh")
                    ) { shader ->
                        shader.setCommonUniforms(data)

                        shader.getUniform("CameraPos")?.setValue(data.camera.pos)
                        shader.getUniform("LightPos")?.setValue(light.absolutePos)
                        shader.getUniform("LightColor")?.setValue(Vector3f(light.color).mul(Vibrancy.config.blockLights.raytraced.brightness))
                        shader.getUniform("LightRadius")?.setValue(light.radius)
                        shader.getUniform("ScreenSize")?.setValue(fbo.width.toFloat(), fbo.height.toFloat())

                        shader.getUniform("ShadowScale")?.setValue(light.shadows.scale)
                        shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture))
                        shader.getUniform("VibrancyShadowSampler")?.setSampler(light.shadows.texture)

                        /*
                        shader.getUniform("ShadowTextureSize")?.setValue(light.shadows.target.width, light.shadows.target.height)
                        shader.getUniform("ShadowMultiplier")?.setValue(
                            if (highQuality)
                                1f - Math.clamp((light.blockPos.center.toVector3f().distance(data.camera.pos) - (Vibrancy.config.blockLights.raytraced.foregroundDistance * 16 - 8)) / 8f, 0f, 1f)
                            else
                                0f
                        )
                        shader.getUniform("VibrancyShadowSampler")?.setSampler(light.shadows.texture)
                         */

                        shader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.worldPosFbo.colorAttachments[0] as GlTexture)
                    }
                )
            )
    }

    val shadows = AsyncBlockShadowTexture(
        16 // TODO
        //Vibrancy.config.blockLights.raytraced.backgroundShadowQuality * 6,
        //Vibrancy.config.blockLights.raytraced.backgroundShadowQuality
    )
    val boxBuffer by lazy {
        val mesh = Mesh(
            NeoVertexFormat.POSITION,
            GlShapeType.QUADS,
            BufferUsage.STATIC_DRAW
        )
        val builder = mesh.Builder()
        builder.cube(boundingBox)
        builder.end()
        return@lazy mesh
    }
    var shadowsDirty = true

    constructor(info: RayPointLightInfo, state: BlockState, pos: BlockPos) : this(
        info.color.apply(state).mul(info.brightness.apply(state), Vector3f()),
        info.radius.apply(state),
        Vector3f(info.offset.apply(state)),
        pos
    )

    override val absolutePos: Vector3f
        get() = Vector3f(blockPos.x.toFloat(), blockPos.y.toFloat(), blockPos.z.toFloat()).add(offset)
    override val boundingBox: AABB
        get() {
            val radius2 = (radius * 2).toDouble()
            return AABB.ofSize(Vec3(absolutePos.x.toDouble(), absolutePos.y.toDouble(), absolutePos.z.toDouble()), radius2, radius2, radius2)
        }
    override val shadowBox: BlockBox
        get() {
            val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.toFloat())).toInt()
            return BlockBox.of(
                BlockPos(blockPos.x - shadowRadius, blockPos.y - shadowRadius, blockPos.z - shadowRadius),
                BlockPos(blockPos.x + shadowRadius, blockPos.y + shadowRadius, blockPos.z + shadowRadius)
            )
        }
    override val shadowPredicate = object : ShadowPredicate {
        override fun shouldCastBlock(
            state: BlockState,
            level: Level,
            pos: BlockPos
        ): Boolean {
            return pos != blockPos && (BlockUtil.INSTANCE.isSolidRender(state, pos, level) || !BlockLightRegistry.has(state.block))
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

            if (sidePos == blockPos) {
                return true
            }

            if (face.step().dot(blockPos.center.subtract(pos.center).toVector3f()) <= 0) {
                return false
            }

            val sideState = level.getBlockState(sidePos)

            return !(BlockUtil.INSTANCE.isSolidRender(state, pos, level) && BlockUtil.INSTANCE.isSolidRender(sideState, sidePos, level))
        }

        override fun isInRange(pos: BlockPos): Boolean {
            val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.toFloat())).toInt()
            return pos.distSqr(blockPos) <= shadowRadius * shadowRadius
        }
    }

    fun reload(manager: LightManager) {
        //shadows.texture.resize(
        //    Vibrancy.config.blockLights.raytraced.backgroundShadowQuality * 6,
        //    Vibrancy.config.blockLights.raytraced.backgroundShadowQuality
        //)
        //shadows.texture.setInterpolation(InterpolationType.LINEAR)
        shadows.rebuildAsync(manager, manager.createShadowMesher(this), this)
    }

    override fun free() {
        shadows.free()
        boxBuffer.free()
    }

    fun render(manager: LightManager, data: RenderEventData, raytrace: Boolean, fbo: GlFramebuffer): LightRenderResult {
        val result = LightRenderResult(
            numRendered = 1,
            numRaytraced = if (raytrace) 1 else 0,
            numShadows = if (raytrace) shadows.size else 0,
            numAsyncTasks = if (shadows.isTaskActive()) 1 else 0
        )

        for (pos in manager.dirtyBlocks) {
            if (manager.getLevel().dimension() == pos.dimension && shadowBox.contains(pos.pos)) {
                shadowsDirty = true
                break
            }
        }

        if (shadowsDirty && raytrace) {
            shadows.rebuildAsync(manager, manager.createShadowMesher(this), this)
            shadowsDirty = false
        }

        if (shadows.checkIfFinished(VertexSorting.byDistance(absolutePos))) {
            val blitSettings = ShadowTexture.blitSettings(data, this)
            blitSettings.bind()
            MeshUtil.SCREEN_MESH.draw()
            blitSettings.unbind()
        }

        GlFlag.POLYGON_OFFSET_FILL.stack.push(true)
        glPolygonOffset(-1f, -1f)

        val meshSettings = meshSettings(fbo, this, data)

        meshSettings.bind()
        shadows.mesh.draw()
        //boxBuffer.draw()
        meshSettings.unbind()

        GlFlag.POLYGON_OFFSET_FILL.stack.pop()

        return result
    }
}