package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlClearBit
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.Vibrancy.isPointingTowards
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.collectors.FloodFillBlockMeshCollector
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.LightTexture
import net.typho.vibrancy.shadows.ShadowBuffer
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
        @JvmStatic
        fun drawState(texture: GlTextureBinding) = GlDrawState.Basic(
            shader = GlShaderShard.FromLocation(
                Vibrancy.id("block/raytraced/blit"),
                { },
                texture
            )
        )
    }

    val blitMesh = Mesh(
        LightMesh.BLIT_VERTEX_FORMAT,
        GlBeginMode.QUADS,
        GlBufferWriter.Mode.REGULAR,
        GlBufferUsage.STREAM_DRAW
    )

    fun blit(target: LightTexture, shadowBuffer: ShadowBuffer, materialTexture: GlTextureBinding) {
        target.framebuffer.bind(NeoRect2i(0, 0, target.width, target.height)).use { fbo ->
            fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, shadowBuffer.glId)

            drawState(materialTexture).bind().use { drawState ->
                drawState.shader.setUniform("LightPos") { set(absolutePos) }
                drawState.shader.setUniform("LightColor") { set(color * VibrancyConfig().rayLightBrightness) }
                drawState.shader.setUniform("LightRadius") { set(radius) }

                blitMesh.draw()
            }
        }
    }

    val meshCollector = FloodFillBlockMeshCollector(pos)

    var meshData: LightMesh.MeshData? = null
        protected set

    val dynamicTexture = LightTexture()
    val dynamicBuffer = ShadowBuffer(GlBufferUsage.STREAM_DRAW)

    val staticTexture = LightTexture()
    val mesh = StaticBlockLightMeshManager { mesh, info ->
        meshData = info
        staticTexture.resize(info.sections.size.x, info.sections.size.y)
        dynamicTexture.resize(info.sections.size.x, info.sections.size.y)
        LightMesh.initBlitMesh(blitMesh, info)
        blit(
            staticTexture,
            mesh.shadowBuffer,
            GlTextureBinding.FromInstance(
                NeoAtlas.blocks,
                GlTextureTarget.TEXTURE_2D
            )
        )
    }

    var shadowsDirty = true

    constructor(info: RayPointLightInfo, state: BlockState, pos: IVec3<Int>) : this(
        info.color(state) * info.brightness(state),
        info.radius(state),
        info.offset(state),
        pos
    )

    override val absolutePos: IVec3<Float>
        get() = pos.toFloat() + offset
    override val boundingBox: AbstractRect3<Int>
        get() = NeoRect3i(pos - radius.toInt(), pos + radius.toInt())
    override val shadowBox: AbstractRect3<Int>
        get() {
            val shadowRadius = ceil(radius.coerceAtMost(VibrancyConfig().rayLightShadowRadius.toFloat())).toInt()
            return NeoRect3i(pos - shadowRadius, pos + shadowRadius)
        }
    val shadowPredicate = object : BlockMeshCollector.Predicate {
        override fun shouldCastBlock(
            level: Level,
            pos: IVec3<Int>,
            state: BlockState?
        ): Boolean {
            return shadowBox.contains(pos) && !BlockLightRegistry.has(state ?: level.getBlockState(pos.blockPos))
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
            return boundingBox.contains(pos)
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

    fun update(manager: LightManager, dynamicShadows: Boolean) {
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

        if (dynamicShadows) {
            manager.getLevel()?.getEntities(null, AABB.ofSize(Vec3(absolutePos.toJOML()), radius.toDouble() * 2, radius.toDouble() * 2, radius.toDouble() * 2))?.let { entities ->
                val quads = arrayListOf<NeoBakedQuad>()
                val builder = object : NeoBakedQuad.Consumer() {
                    override fun take(quad: NeoBakedQuad) {
                        quads.add(quad)
                    }
                }
                val tickDelta = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(true)

                for (entity in entities) {
                    Minecraft.getInstance().entityRenderDispatcher.render(
                        entity,
                        Mth.lerp(tickDelta.toDouble(), entity.xOld, entity.x),
                        Mth.lerp(tickDelta.toDouble(), entity.yOld, entity.y),
                        Mth.lerp(tickDelta.toDouble(), entity.zOld, entity.z),
                        Mth.lerp(tickDelta, entity.yRotO, entity.yRot),
                        tickDelta,
                        PoseStack(),
                        { WrapperUtil.INSTANCE.unwrap(builder) },
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT
                    )
                }

                if (quads.isNotEmpty()) {
                    dynamicBuffer.lazyUploadQuads(quads)()
                    blit(
                        dynamicTexture,
                        dynamicBuffer,
                        GlTextureBinding.FromInstance(
                            NeoAtlas.blocks, // TODO
                            GlTextureTarget.TEXTURE_2D
                        )
                    )
                }
            }
        } else {
            dynamicTexture.framebuffer.bind(NeoRect2i(0, 0, dynamicTexture.width, dynamicTexture.height)).use { fbo ->
                fbo.clear(GlClearBit.Color(NeoColor.FULL_ON))
            }
        }
    }

    fun render(shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit) {
        debugOut("lightsRendered", 1)

        if (mesh.isTaskActive()) {
            debugOut("numAsyncTasks", 1)
        }

        shader.setUniform("LightPos") { set(absolutePos) }
        shader.setUniform("LightColor") { set(color) }
        shader.setUniform("LightRadius") { set(radius) }
        shader.setTexture(1, GlTextureBinding.FromInstance(
            staticTexture,
            GlTextureTarget.TEXTURE_2D
        ))
        shader.setTexture(2, GlTextureBinding.FromInstance(
            dynamicTexture,
            GlTextureTarget.TEXTURE_2D
        ))
        mesh.lightMesh.draw()
    }
}