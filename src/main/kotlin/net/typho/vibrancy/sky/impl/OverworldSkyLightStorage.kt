package net.typho.vibrancy.sky.impl

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlAlphaFunction
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlClearBit
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlCullFace
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlCullShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDepthShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.NeoVec4f
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.collectors.BlockMeshCollector
import net.typho.vibrancy.collectors.SkyLightBlockMeshCollector
import net.typho.vibrancy.shadows.LightFace
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.LightTexture
import net.typho.vibrancy.sky.ChunkedSkyLightStorage
import net.typho.vibrancy.sky.SkyLightStorage
import net.typho.vibrancy.util.VibrancyThreadPool
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.glClearDepth
import org.lwjgl.system.NativeResource
import java.util.concurrent.CompletableFuture
import kotlin.collections.addAll
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class OverworldSkyLightStorage : ChunkedSkyLightStorage<OverworldSkyLightInfo, OverworldSkyLightStorage.Chunk>(OverworldSkyLightType) {
    companion object {
        @JvmField
        val shadowDrawState = GlDrawState.Basic(
            cull = GlCullShard.Enabled(
                GlCullFace.BACK
            ),
            depth = GlDepthShard.Enabled(
                GlAlphaFunction.GEQUAL
            ),
            shader = GlShaderShard.FromLocation(
                Vibrancy.id("sky/overworld/blit"),
                { },
                GlTextureBinding.FromInstance(
                    NeoAtlas.blocks,
                    GlTextureTarget.TEXTURE_2D
                )
            )
        )
        @JvmField
        val lightDrawState = LightMesh.drawState(
            NeoAtlas.blocks,
            Vibrancy.id("sky/overworld/mesh")
        )
    }

    var info: OverworldSkyLightInfo? = null
        private set
    @JvmField
    val texture = LightTexture.Shadow().also { it.resize(16384, 16384) }

    override fun createChunk(
        manager: LightManager,
        pos: ChunkPos
    ) = Chunk(pos)

    override fun load(
        manager: LightManager,
        info: OverworldSkyLightInfo
    ) {
        this.info = info
    }

    @Suppress("SENSELESS_COMPARISON")
    fun render(data: RenderEventData, manager: LightManager, result: GlFramebuffer, temp: GlFramebuffer) {
        for ((pos, chunk) in chunks) {
            chunk.update(data, manager)
            chunk.checkIfFinished()
        }

        var lightAngle = (data.level!!.getSunAngle(Vibrancy.tickDelta) + PI.toFloat() / 2) % (PI.toFloat() * 2)
        var lightColor = info!!.sunColor

        if (lightAngle > PI.toFloat()) {
            lightAngle -= PI.toFloat()
            lightColor = info!!.moonColor * data.level!!.moonBrightness
        } else {
            val sunriseColor = data.level!!.effects().getSunriseColor(data.level!!.getTimeOfDay(Vibrancy.tickDelta), Vibrancy.tickDelta)

            if (sunriseColor != null) {
                lightColor = lightColor.lerp(sunriseColor[0], sunriseColor[1], sunriseColor[2], sunriseColor[3] * 0.75f)
            }
        }

        lightColor *= sqrt(sin(lightAngle).coerceAtLeast(0f)) * info!!.brightness

        //val lightColor = sunColor * sin(lightAngle).coerceAtLeast(0f) + moonColor * sin(lightAngle + PI.toFloat()).coerceAtLeast(0f)

        // TODO
        //.translate((-data.camera.pos.toInt().toFloat()).toJOML())
        val shadowRot = Quaternionf()
            .rotateX(lightAngle)
            .rotateY(-PI.toFloat() / 2)
            .rotateY(Math.toRadians(15.0).toFloat())
        val shadowMat = Matrix4f().rotate(shadowRot)
            .scale(0.005f)

        texture.framebuffer.bind(NeoRect2i(0, 0, texture.width, texture.height)).use { fbo ->
            fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF), GlClearBit.Depth(0f))
            glClearDepth(1.0)

            shadowDrawState.bind().use { settings ->
                settings.shader.setUniform("ShadowMat") { set(shadowMat) }

                for ((pos, chunk) in chunks) {
                    chunk.mesh.draw()
                }
            }
        }

        temp.bind().use { fbo ->
            fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))

            lightDrawState.bind().use { settings ->
                settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((-data.camera.pos.toFloat()).toJOML(), Matrix4f())) }
                settings.shader.setUniform("ProjMat") { set(data.projMat) }
                settings.shader.setUniform("ShadowMat") { set(shadowMat) }

                settings.shader.setUniform("FogStart") { set(RenderSystem.getShaderFogStart()) }
                settings.shader.setUniform("FogEnd") { set(RenderSystem.getShaderFogEnd()) }
                settings.shader.setUniform("FogShape") { set(RenderSystem.getShaderFogShape().index) }

                settings.shader.setUniform("CameraPos") { setFloatVec(data.camera.pos) }
                settings.shader.setUniform("LightColor") { setFloatVec(lightColor) }
                settings.shader.setUniform("LightDirection") { setFloatVec(NeoVec4f(shadowRot.transform(Vector4f(0f, -1f, 0f, 0f))).xyz) }

                settings.shader.setTexture(
                    1,
                    GlTextureBinding.FromInstance(
                        texture,
                        GlTextureTarget.TEXTURE_2D
                    )
                )
                settings.shader.setTexture(
                    2,
                    GlTextureBinding.FromInstance(
                        texture.depth!!,
                        GlTextureTarget.TEXTURE_2D
                    )
                )

                for ((pos, chunk) in chunks) {
                    chunk.mesh.draw()
                }
            }
        }

        manager.blitFromTemp(result, temp)
    }

    class Chunk(
        @JvmField
        val pos: ChunkPos
    ) : SkyLightStorage<OverworldSkyLightInfo>, NativeResource {
        @JvmField
        val mesh = LightMesh(GlBufferUsage.STATIC_DRAW)
        private var dirty = true
        private var asyncTask: CompletableFuture<() -> Unit>? = null

        override fun free() {
            mesh.free()
            asyncTask?.cancel(true)
        }

        fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

        fun checkIfFinished(): Boolean {
            asyncTask?.let { task ->
                try {
                    if (task.isDone) {
                        task.get()()
                        asyncTask = null
                        return true
                    }
                } catch (e: NullPointerException) {
                    asyncTask = null
                }
            }

            return false
        }

        private fun rebuildBlocksAsyncImpl(
            manager: LightManager
        ): () -> Unit {
            val level = manager.getLevel() ?: throw NullPointerException("No level?")

            val lightFaces = arrayListOf<LightFace>()
            SkyLightBlockMeshCollector(pos).submit(
                manager,
                level,
                NeoAtlas.blocks,
                object : BlockMeshCollector.Consumer {
                    override val predicate: BlockMeshCollector.Predicate = object : BlockMeshCollector.Predicate {
                        override fun shouldCastBlock(
                            level: Level,
                            pos: IVec3<Int>,
                            state: BlockState?
                        ): Boolean {
                            return true
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

                            val state = state ?: level.getBlockState(pos.blockPos)

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
                    }

                    override fun collect(faces: Iterable<LightFace>) {
                        lightFaces.addAll(faces)
                    }
                }
            )

            return mesh.lazyUploadNoAtlas(lightFaces)
        }

        fun rebuildBlocksAsync(
            data: RenderEventData,
            manager: LightManager
        ) {
            if (VibrancyConfig.useMultithreading) {
                asyncTask?.cancel(true)
                asyncTask = VibrancyThreadPool.submit(data, pos, manager) { rebuildBlocksAsyncImpl(manager) }
            } else {
                rebuildBlocksAsyncImpl(manager)()
            }
        }

        fun update(data: RenderEventData, manager: LightManager) {
            if (dirty) {
                rebuildBlocksAsync(data, manager)
                dirty = false
            }
        }

        override fun load(
            manager: LightManager,
            info: OverworldSkyLightInfo
        ) {
            dirty = true
        }

        override fun reload(manager: LightManager) {
            dirty = true
        }

        override fun loadChunk(
            manager: LightManager,
            chunk: ChunkAccess
        ) {
            dirty = true
        }

        override fun deloadChunk(
            manager: LightManager,
            chunk: ChunkAccess
        ) {
            free()
        }

        override fun clear(manager: LightManager) {
            dirty = true
        }
    }
}