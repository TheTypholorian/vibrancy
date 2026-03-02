package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.buffers.Mesh
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexFormat
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
                    BindBufferBaseShard(
                        { light.shadows.atlas },
                        0
                    ),
                    ShaderShard(
                        Vibrancy.id("block/raytraced/mesh")
                    ) { shader ->
                        shader.setCommonUniforms(data)

                        shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture))
                        shader.getUniform("VibrancyShadowSampler")?.setSampler(light.shadows.texture)
                    }
                )
            )
    }

    val shadows = AsyncBlockShadowTexture()
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

        override fun isInShadowRange(pos: BlockPos): Boolean {
            val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.toFloat())).toInt()
            return pos.distSqr(blockPos) <= shadowRadius * shadowRadius
        }

        override fun isInLightRange(pos: BlockPos): Boolean {
            val shadowRadius = ceil(radius).toInt()
            return pos.distSqr(blockPos) <= shadowRadius * shadowRadius
        }
    }

    fun reload(manager: LightManager) {
        shadows.rebuildAsync(manager, manager.createShadowMesher(this), this)
    }

    override fun free() {
        shadows.free()
        boxBuffer.free()
    }

    fun render(manager: LightManager, data: RenderEventData, fbo: GlFramebuffer): LightRenderResult {
        val result = LightRenderResult(
            numRendered = 1,
            numAsyncTasks = if (shadows.isTaskActive()) 1 else 0
        )

        for (pos in manager.dirtyBlocks) {
            if (manager.getLevel()?.dimension() == pos.dimension && shadowBox.contains(pos.pos)) {
                shadowsDirty = true
                break
            }
        }

        if (shadowsDirty) {
            shadows.rebuildAsync(manager, manager.createShadowMesher(this), this)
            shadowsDirty = false
        }

        if (shadows.checkIfFinished()) { // VertexSorting.byDistance(absolutePos)
            val blitSettings = ShadowTexture.blitSettings(data, this)
            blitSettings.bind()
            MeshUtil.SCREEN_MESH.draw()
            blitSettings.unbind()
        }

        GlFlag.POLYGON_OFFSET_FILL.stack.push(true)
        glPolygonOffset(-1f, -1f)

        val meshSettings = meshSettings(fbo, this, data)

        meshSettings.bind()
        shadows.lightMesh.draw()
        meshSettings.unbind()

        GlFlag.POLYGON_OFFSET_FILL.stack.pop()

        return result
    }
}