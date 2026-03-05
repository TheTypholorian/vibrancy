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
import net.typho.big_shot_lib.api.client.opengl.util.MeshUtil
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.Vibrancy.isPointingTowards
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.shadows.AsyncBlockShadowMesh
import net.typho.vibrancy.shadows.ShadowPredicate
import net.typho.vibrancy.util.PointLight
import org.joml.Vector3f
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
        fun meshBlitSettings(data: RenderEventData, light: RayPointLight) = RenderSettings(
            Vibrancy.id("block/raytraced/blit"),
            listOf(
                DisableFlagsShard(listOf(
                    GlFlag.DEPTH_TEST,
                    GlFlag.CULL_FACE,
                    GlFlag.BLEND
                )),
                BindBufferBaseShard(
                    { light.shadows.lightMesh.mesh.vbo.cast(BufferType.SHADER_STORAGE_BUFFER) },
                    0
                ),
                BindBufferBaseShard(
                    { light.shadows.lightMesh.atlas },
                    1
                ),
                BindBufferBaseShard(
                    { light.shadows.shadowMesh.vbo.cast(BufferType.SHADER_STORAGE_BUFFER) },
                    2
                ),
                FramebufferShard(
                    { light.shadows.lightMesh.target },
                    true,
                    ClearBit.Color(IColor.FULL_OFF)
                ),
                ShaderShard(
                    Vibrancy.id("block/raytraced/blit")
                ) { shader ->
                    shader.setCommonUniforms(data)
                    shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.blockAtlas)

                    shader.getUniform("LightPos")?.setValue(light.absolutePos)
                    shader.getUniform("LightColor")?.setValue(light.color)
                    shader.getUniform("LightRadius")?.setValue(light.radius)
                    shader.getUniform("LightBrightness")?.setValue(Vibrancy.config.blockLights.raytraced.brightness)
                }
            )
        )
    }

    val shadows = AsyncBlockShadowMesh()
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
            level: Level,
            pos: BlockPos
        ): Boolean {
            return shadowBox.contains(pos) && (pos == blockPos || !BlockLightRegistry.has(level.getBlockState(pos).block))
        }

        override fun shouldCastFluid(
            level: Level,
            pos: BlockPos
        ): Boolean {
            return !BlockLightRegistry.has(level.getBlockState(pos).block)
        }

        override fun shouldCastFace(
            face: Direction?,
            level: Level,
            pos: BlockPos
        ): Boolean {
            if (face == null || pos == blockPos) {
                return true
            }

            val sidePos = pos.relative(face)

            if (sidePos == blockPos) {
                return true
            }

            if (!face.isPointingTowards(pos, blockPos)) {
                return false
            }

            if (
                BlockUtil.INSTANCE.isSolidRender(level.getBlockState(pos), pos, level) && BlockUtil.INSTANCE.isSolidRender(level.getBlockState(sidePos), sidePos, level)
                //!Block.shouldRenderFace( // TODO
                //    state,
                //    level,
                //    pos,
                //    face,
                //    sidePos
                //)
            ) {
                return false
            }

            return true
        }

        override fun isInShadowRange(pos: BlockPos): Boolean {
            if (pos == blockPos) {
                return false
            }

            val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.toFloat())).toInt()
            return pos.distSqr(blockPos) <= shadowRadius * shadowRadius
        }

        override fun isInLightRange(pos: BlockPos): Boolean {
            return pos.distSqr(blockPos) <= radius * radius
        }
    }

    fun reload(manager: LightManager) {
        shadowsDirty = true
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
            if (!shadows.lightMesh.empty) {
                val blitSettings = meshBlitSettings(data, this)
                blitSettings.bind()
                MeshUtil.SCREEN_MESH.draw()
                blitSettings.unbind()
            }
        }

        shadows.lightMesh.draw(fbo, data, TextureUtil.INSTANCE.blockAtlas)

        return result
    }
}