package net.typho.vibrancy.sky.impl

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferType
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.ClearBit
import net.typho.big_shot_lib.api.client.opengl.buffers.Mesh
import net.typho.big_shot_lib.api.client.opengl.shaders.GlShader
import net.typho.big_shot_lib.api.client.opengl.shaders.NeoShaderRegistry
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.shadows.AsyncBlockShadowMesh
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.LightMesh.Companion.BLIT_VERTEX_FORMAT
import net.typho.vibrancy.shadows.ShadowPredicate
import net.typho.vibrancy.shadows.SkyLightMesher
import net.typho.vibrancy.sky.ChunkedSkyLightStorage
import net.typho.vibrancy.sky.SkyLightStorage
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.NativeResource
import kotlin.math.cos
import kotlin.math.sin

class OverworldSkyLightStorage : ChunkedSkyLightStorage<OverworldSkyLightInfo, OverworldSkyLightStorage.Chunk>(OverworldSkyLightType) {
    companion object {
        @JvmStatic
        fun meshBlitSettings(data: RenderEventData, storage: OverworldSkyLightStorage) = RenderSettings(
            Vibrancy.id("sky/overworld/blit"),
            listOf(
                DisableFlagsShard(listOf(
                    GlFlag.DEPTH_TEST,
                    GlFlag.CULL_FACE
                )),
                BlendShard(
                    true,
                    IColor.FULL_ON,
                    BlendEquation.ADD,
                    BlendFunction.Basic(
                        BlendFactor.DST_COLOR,
                        BlendFactor.ZERO
                    )
                ),
                ShaderShard(
                    Vibrancy.id("sky/overworld/blit")
                ) { shader ->
                    shader.setCommonUniforms(data)
                    shader.getUniform("ProjMat")?.setValue(Matrix4f(data.projMat))
                    shader.getUniform("ModelViewMat")?.setValue(Matrix4f(data.modelViewMat))
                    shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.blockAtlas)

                    shader.getUniform("LightDirection")?.setValue(storage.getLightDirection(data.level))
                    //shader.getUniform("LightBrightness")?.setValue(Vibrancy.config.blockLights.raytraced.brightness) // TODO
                }
            )
        )

        @JvmStatic
        fun chunkMeshBlitSettings(chunk: Chunk) = RenderSettings(
            Vibrancy.id("sky/overworld/blit_chunk"),
            listOf(
                BindBufferBaseShard(
                    { chunk.mesh.shadowMesh.vbo.cast(BufferType.SHADER_STORAGE_BUFFER) },
                    0
                ),
                FramebufferShard(
                    { chunk.mesh.lightMesh.value!!.target },
                    true
                )
            )
        )
    }

    var lightInfo: OverworldSkyLightInfo? = null
        private set

    fun getLightDirection(level: Level): Vector3f {
        val sunAngle = level.getSunAngle(0f)
        var x = -sin(sunAngle)
        var y = cos(sunAngle)

        if (y < 0) {
            x = -x
            y = -y
        }

        return Vector3f(x, y, 0f)
    }

    fun getLightColor(level: Level): IColor {
        val y = cos(level.getSunAngle(0f))
        return if (y > 0) {
            IColor.RGBF(lightInfo!!.sunColor.mul(y, Vector3f()))
        } else {
            IColor.RGBF(lightInfo!!.moonColor.mul(-y, Vector3f()))
        }
    }

    override fun createChunk(
        manager: LightManager,
        level: Level,
        pos: ChunkPos
    ): Chunk = Chunk(pos)

    override fun load(
        manager: LightManager,
        info: OverworldSkyLightInfo
    ) {
        this.lightInfo = info
    }

    inner class Chunk(
        @JvmField
        val pos: ChunkPos
    ) : SkyLightStorage<OverworldSkyLightInfo>, NativeResource, ShadowPredicate {
        private var shadowsDirty = true
        @JvmField
        val mesh: AsyncBlockShadowMesh<*> = AsyncBlockShadowMesh(SkyLightMesher(pos)) { info, data ->
            LightMesh.initBlitMesh(blitMesh, info)
        }
        @JvmField
        val blitMesh = Mesh(
            BLIT_VERTEX_FORMAT,
            GlShapeType.QUADS,
            BufferUsage.STREAM_DRAW
        )

        fun updateShadows(manager: LightManager, data: RenderEventData, debugOut: (key: String, value: Int) -> Unit) {
            if (shadowsDirty) {
                mesh.rebuildAsync(manager, data, this)
                shadowsDirty = false
            }

            mesh.checkIfFinished(data)

            if (mesh.isTaskActive()) {
                debugOut("asyncTasks", 1)
            }

            mesh.lightMesh.value!!.target.clear(ClearBit.Color(getLightColor(data.level)))

            val shader = NeoShaderRegistry.get(Vibrancy.id("sky/overworld/blit"))!!

            val blitSettings = chunkMeshBlitSettings(this)
            blitSettings.bind()
            shader.getUniform("LightOffset")?.setValue(0f, 0f, 0f)
            blitMesh.draw()
            blitSettings.unbind()

            // TODO
            /*
            chunks[if (getLightDirection(data.level).x > 0) ChunkPos(pos.x + 1, pos.z) else ChunkPos(pos.x - 1, pos.z)]?.let { chunk ->
                val blitSettings = chunkMeshBlitSettings(chunk)
                blitSettings.bind()
                shader.getUniform("LightOffset")?.setValue(if (getLightDirection(data.level).x > 0) -16f else 16f, 0f, 0f)
                blitMesh.draw()
                blitSettings.unbind()
            }
             */
        }

        fun render(shader: GlShader, debugOut: (key: String, value: Int) -> Unit) {
            if (lightInfo != null) {
                mesh.lightMesh.value!!.draw(shader)
                debugOut("chunksRendered", 1)
            }
        }

        override fun shouldCastBlock(
            level: Level,
            pos: BlockPos,
            state: BlockState
        ): Boolean {
            return level.getBrightness(LightLayer.SKY, pos) > 0 || level.getBrightness(LightLayer.SKY, pos.above()) > 0 || level.getBrightness(LightLayer.SKY, if (getLightDirection(level).x > 0) pos.east() else pos.west()) > 0
        }

        override fun shouldCastFace(
            face: Direction?,
            level: Level,
            pos: BlockPos,
            state: BlockState
        ): Boolean {
            if (face == null) {
                return true
            }

            if (face == Direction.DOWN) {
                return false
            }

            return BlockUtil.INSTANCE.shouldRenderFace(
                level,
                pos,
                face,
                state
            )
        }

        override fun isInLightRange(pos: BlockPos): Boolean {
            return true
        }

        override fun isInShadowRange(pos: BlockPos): Boolean {
            return true
        }

        override fun load(
            manager: LightManager,
            info: OverworldSkyLightInfo
        ) {
            shadowsDirty = true
        }

        override fun reload(manager: LightManager) {
            shadowsDirty = true
        }

        override fun loadChunk(
            manager: LightManager,
            chunk: LevelChunk
        ) {
            shadowsDirty = true
        }

        override fun deloadChunk(
            manager: LightManager,
            chunk: LevelChunk
        ) {
            mesh.free()
        }

        override fun clear(manager: LightManager) {
            mesh.free()
        }

        override fun free() {
            mesh.free()
        }
    }
}