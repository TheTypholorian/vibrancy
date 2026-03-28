package net.typho.vibrancy.block.impl

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.Mesh
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexFormat
import net.typho.big_shot_lib.api.client.opengl.shaders.GlShader
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.Vibrancy.isPointingTowards
import net.typho.vibrancy.Vibrancy.toBlockBox
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.shadows.AsyncBlockShadowMesh
import net.typho.vibrancy.shadows.FloodFillMesher
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.ShadowPredicate
import net.typho.vibrancy.util.PointLight
import org.joml.Matrix4f
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
                    { light.shadows.shadowBuffer },
                    0
                ),
                FramebufferShard(
                    { light.shadows.lightMesh.value!!.target },
                    true
                ),
                ShaderShard(
                    Vibrancy.id("block/raytraced/blit")
                ) { shader ->
                    shader.setCommonUniforms(data)
                    shader.getUniform("ProjMat")?.setValue(Matrix4f(data.projMat))
                    shader.getUniform("ModelViewMat")?.setValue(Matrix4f(data.modelViewMat))
                    shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.blockAtlas)

                    shader.getUniform("LightPos")?.setValue(light.absolutePos)
                    shader.getUniform("LightColor")?.setValue(light.color.mul(Vibrancy.config.blockLights.raytraced.brightness, Vector3f()))
                    shader.getUniform("LightRadius")?.setValue(light.radius)
                }
            )
        )
    }

    val shadows: AsyncBlockShadowMesh<FloodFillMesher> = AsyncBlockShadowMesh(FloodFillMesher(blockPos)) { info, data ->
        val blitSettings = meshBlitSettings(data, this)
        blitSettings.bind()
        LightMesh.blitLight(info)
        blitSettings.unbind()
        data.target.viewport() // TODO
    }
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
            pos: BlockPos,
            state: BlockState
        ): Boolean {
            return boundingBox.toBlockBox().contains(pos) && !BlockLightRegistry.has(state)
        }

        override fun shouldCastFace(
            face: Direction?,
            level: Level,
            pos: BlockPos,
            state: BlockState
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
                !BlockUtil.INSTANCE.shouldRenderFace(
                    level,
                    pos,
                    face,
                    state
                )
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
            val lightRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.lightRadius.toFloat())).toInt()
            return pos.distSqr(blockPos) <= lightRadius * lightRadius
        }
    }

    fun reload() {
        synchronized(shadows.mesher) {
            shadows.mesher.markAllDirty()
        }
        shadowsDirty = true
    }

    override fun free() {
        shadows.free()
        boxBuffer.free()
    }

    fun update(manager: LightManager, data: RenderEventData) {
        for (pos in manager.dirtyBlocks) {
            if (boundingBox.toBlockBox().contains(pos)) {
                synchronized(shadows.mesher) {
                    shadows.mesher.markDirty(pos)
                }
                shadowsDirty = true
                break
            }
        }

        if (shadowsDirty) {
            shadows.rebuildAsync(manager, data, shadowPredicate)
            shadowsDirty = false
        }

        shadows.checkIfFinished(data)
    }

    fun render(shader: GlShader, debugOut: (key: String, value: Int) -> Unit) {
        debugOut("lightsRendered", 1)

        if (shadows.isTaskActive()) {
            debugOut("numAsyncTasks", 1)
        }

        shader.getUniform("LightPos")?.setValue(absolutePos) // TODO
        shader.getUniform("LightColor")?.setValue(color)
        shader.getUniform("LightRadius")?.setValue(radius)
        shadows.lightMesh.value!!.draw(shader)
    }
}