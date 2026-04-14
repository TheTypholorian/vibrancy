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
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.Vibrancy.isPointingTowards
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.collectors.FloodFillBlockMeshCollector
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.StaticBlockLightMeshManager
import net.typho.vibrancy.util.PointLight
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.system.NativeResource
import kotlin.math.ceil

open class RayPointLight(
    @JvmField
    val color: IVec3<Float>,
    @JvmField
    val radius: Float,
    @JvmField
    val offset: IVec3<Float>,
    override val pos: IVec3<Int>
) : PointLight, NativeResource {
    companion object {
        @JvmField
        val drawState = GlDrawState.Basic(
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

    val meshCollector = FloodFillBlockMeshCollector(pos)
    val mesh = StaticBlockLightMeshManager { mesh, info ->
        NeoGlFramebuffer().use { fbo ->
            fbo.bind(NeoRect2i(0, 0, mesh.lightMesh.texture.width, mesh.lightMesh.texture.height)).use { fbo ->
                fbo.colorAttachments[0] = mesh.lightMesh.texture
                fbo.checkStatus().throwIfError()

                fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, mesh.shadowBuffer.glId)

                drawState.bind().use { drawState ->
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
                        mesh.draw()
                    }
                }
            }
        }
    }
    var shadowsDirty = true

    constructor(info: RayPointLightInfo, state: BlockState, pos: IVec3<Int>) : this(
        info.color.apply(state) * info.brightness.apply(state),
        info.radius.apply(state),
        info.offset.apply(state),
        pos
    )

    override val absolutePos: IVec3<Float>
        get() = pos.toFloat() + offset
    override val boundingBox: AbstractRect3<Int>
        get() = NeoRect3i(pos - radius.toInt(), pos + radius.toInt())
    override val shadowBox: AbstractRect3<Int>
        get() {
            val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.raytraced.shadowRadius.toFloat())).toInt()
            return NeoRect3i(pos - shadowRadius, pos + shadowRadius)
        }
    val shadowPredicate = object : BlockMeshCollector.Predicate {
        override fun shouldCastBlock(
            level: Level,
            pos: IVec3<Int>,
            state: BlockState?
        ): Boolean {
            return shadowBox.contains(pos)
        }

        override fun shouldCastFace(
            face: NeoDirection?,
            level: Level,
            pos: IVec3<Int>,
            state: BlockState?
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
                    state ?: level.getBlockState(pos.blockPos)
                )
            ) {
                return false
            }

            return true
        }
    }
    val lightPredicate = object : BlockMeshCollector.Predicate {
        override fun shouldCastBlock(
            level: Level,
            pos: IVec3<Int>,
            state: BlockState?
        ): Boolean {
            return boundingBox.contains(pos) && !BlockLightRegistry.has(state ?: level.getBlockState(pos.blockPos))
        }

        override fun shouldCastFace(
            face: NeoDirection?,
            level: Level,
            pos: IVec3<Int>,
            state: BlockState?
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
                    state ?: level.getBlockState(pos.blockPos)
                )
            ) {
                return false
            }

            return true
        }
    }

    fun reload() {
        synchronized(meshCollector) {
            meshCollector.markAllDirty()
        }
        shadowsDirty = true
    }

    override fun free() {
        mesh.free()
    }

    fun update(manager: LightManager) {
        synchronized(meshCollector) {
            for (pos in manager.dirtyBlocks) {
                if (boundingBox.contains(pos)) {
                    shadowsDirty = shadowsDirty or meshCollector.markDirty(pos)
                }
            }
        }

        if (shadowsDirty) {
            mesh.rebuildBlocksAsync(manager, meshCollector, shadowPredicate, lightPredicate)
            shadowsDirty = false
        }

        mesh.checkIfFinished()
    }

    fun render(shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit) {
        debugOut("lightsRendered", 1)

        if (mesh.isTaskActive()) {
            debugOut("numAsyncTasks", 1)
        }

        shader.setUniform("LightPos") { set(absolutePos) }
        shader.setUniform("LightColor") { set(color) }
        shader.setUniform("LightRadius") { set(radius) }
        mesh.lightMesh.draw(shader)
    }
}