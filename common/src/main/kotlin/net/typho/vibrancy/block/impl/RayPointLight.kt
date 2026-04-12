package net.typho.vibrancy.block.impl

import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlClearBit
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.plus
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.toJOML
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.Vibrancy.isPointingTowards
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.shadows.AsyncBlockShadowMesh
import net.typho.vibrancy.shadows.FloodFillMesher
import net.typho.vibrancy.shadows.LightFacePredicate
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.util.PointLight
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.system.NativeResource
import kotlin.math.ceil

open class RayPointLight(
    @JvmField
    val color: AbstractVec3<Float>,
    @JvmField
    val radius: Float,
    @JvmField
    val offset: AbstractVec3<Float>,
    override val pos: AbstractVec3<Int>
) : PointLight, LightFacePredicate, NativeResource {
    companion object {
        @JvmField
        val blitDrawState = GlDrawState.Basic(
            /*blend = GlBlendShard.Enabled(
                BlendFunction.Basic(
                    GlBlendingFactor.ONE,
                    GlBlendingFactor.ONE_MINUS_SRC_ALPHA
                ),
                GlBlendEquation.ADD
            ),*/
            shader = GlShaderShard.FromLocation(
                Vibrancy.id("block/raytraced/blit"),
                { },
                GlTextureBinding.FromInstance(
                    NeoAtlas.blocks,
                    GlTextureTarget.TEXTURE_2D
                )
            )
        )
    }

    val shadows: AsyncBlockShadowMesh<FloodFillMesher> = AsyncBlockShadowMesh(FloodFillMesher(pos)) { info ->
        NeoGlFramebuffer().use { fbo ->
            fbo.bind(NeoRect2i(0, 0, shadows.lightMesh.texture.width, shadows.lightMesh.texture.height)).use { fbo ->
                fbo.colorAttachments[0] = shadows.lightMesh.texture
                fbo.checkStatus().throwIfError()

                fbo.clear(GlClearBit.Color(NeoColor.FULL_ON))
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, shadows.shadowMesh.vbo.glId)

                blitDrawState.bind().use { drawState ->
                    drawState.shader.setUniform("LightPos") { set(absolutePos) }
                    drawState.shader.setUniform("LightColor") { set(color * Vibrancy.config.blockLights.raytraced.brightness) }
                    drawState.shader.setUniform("LightRadius") { set(radius) }

                    Mesh(
                        LightMesh.BLIT_VERTEX_FORMAT,
                        GlBeginMode.QUADS,
                        GlBufferWriter.Mode.REGULAR,
                        GlBufferUsage.STREAM_DRAW
                    ).use { mesh ->
                        LightMesh.initBlitMesh(mesh, info)
                        mesh.drawInstanced(shadows.shadowMesh.size / 6)
                    }
                }
            }
        }
    }
    var shadowsDirty = true

    constructor(info: RayPointLightInfo, state: BlockState, pos: AbstractVec3<Int>) : this(
        info.color.apply(state) * info.brightness.apply(state),
        info.radius.apply(state),
        info.offset.apply(state),
        pos
    )

    override val absolutePos: AbstractVec3<Float>
        get() = pos.toFloat() + offset
    override val boundingBox: AbstractRect3<Int>
        get() {
            return NeoRect3i(pos - radius.toInt(), pos + radius.toInt())
        }
    override val shadowBox: AbstractRect3<Int>
        get() {
            val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.toFloat())).toInt()
            return NeoRect3i(pos - shadowRadius, pos + shadowRadius)
        }

    override fun shouldCastBlock(
        level: Level,
        pos: AbstractVec3<Int>,
        state: BlockState
    ): Boolean {
        return boundingBox.contains(pos)
    }

    override fun shouldCastFace(
        face: NeoDirection?,
        level: Level,
        pos: AbstractVec3<Int>,
        state: BlockState
    ): Boolean {
        if (face == null) {
            return true
        }

        val sidePos = pos + face

        if (sidePos == this@RayPointLight.pos) {
            return true
        }

        if (!face.isPointingTowards(pos, this@RayPointLight.pos)) {
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

    fun reload() {
        synchronized(shadows.mesher) {
            shadows.mesher.markAllDirty()
        }
        shadowsDirty = true
    }

    override fun free() {
        shadows.free()
    }

    fun update(manager: LightManager, data: RenderEventData) {
        synchronized(shadows.mesher) {
            for (pos in manager.dirtyBlocks) {
                if (boundingBox.contains(pos)) {
                    shadowsDirty = shadowsDirty or shadows.mesher.markDirty(pos)
                }
            }
        }

        if (shadowsDirty) {
            shadows.rebuildAsync(manager, this) { face ->
                (face.quad.v0.normal?.let { it.toJOML().dot((absolutePos - face.quad.v0.pos).toJOML()) > 0f } ?: true) &&
                face.blockPos.inDistance(
                    pos,
                    Vibrancy.config.blockLights.raytraced.shadowRadius
                ) && !BlockLightRegistry.has(face.state)
            }
            shadowsDirty = false
        }

        shadows.checkIfFinished()
    }

    fun render(shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit) {
        debugOut("lightsRendered", 1)

        if (shadows.isTaskActive()) {
            debugOut("numAsyncTasks", 1)
        }

        shader.setUniform("LightPos") { set(absolutePos) }
        shader.setUniform("LightColor") { set(color) }
        shader.setUniform("LightRadius") { set(radius) }
        shadows.lightMesh.draw(shader)
    }
}