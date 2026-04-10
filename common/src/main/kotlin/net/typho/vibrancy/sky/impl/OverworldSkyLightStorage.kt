package net.typho.vibrancy.sky.impl

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendEquation
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendingFactor
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlBlendShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.blockPos
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.plus
import net.typho.big_shot_lib.api.math.vec.NeoVec3f
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.shadows.AsyncBlockShadowMesh
import net.typho.vibrancy.shadows.LightFacePredicate
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.LightMesh.Companion.BLIT_VERTEX_FORMAT
import net.typho.vibrancy.shadows.SkyLightMesher
import net.typho.vibrancy.sky.ChunkedSkyLightStorage
import net.typho.vibrancy.sky.SkyLightStorage
import org.lwjgl.system.NativeResource
import kotlin.math.cos
import kotlin.math.sin

class OverworldSkyLightStorage : ChunkedSkyLightStorage<OverworldSkyLightInfo, OverworldSkyLightStorage.Chunk>(OverworldSkyLightType) {
    companion object {
        @JvmField
        val meshBlitDrawState = GlDrawState.Basic(
            blend = GlBlendShard.Enabled(
                BlendFunction.Basic(
                    GlBlendingFactor.DST_COLOR,
                    GlBlendingFactor.ZERO
                ),
                GlBlendEquation.ADD
            ),
            shader = GlShaderShard.FromLocation(
                Vibrancy.id("sky/overworld/blit"),
                { }
            )
        )
        /*= RenderSettings(
            Vibrancy.id("sky/overworld/blit"),
            listOf(
                DisableFlagsShard(listOf(
                    GlFlag.DEPTH_TEST,
                    GlFlag.CULL_FACE
                )),
                BlendShard(
                    true,
                    NeoColor.FULL_ON,
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
                    shader.getUniform("Sampler0")?.setSampler(NeoAtlas.blocks)

                    shader.getUniform("LightDirection")?.setValue(storage.getLightDirection(data.level))
                    //shader.getUniform("LightBrightness")?.setValue(Vibrancy.config.blockLights.raytraced.brightness) // TODO
                }
            )
        )
         */
    }

    var lightInfo: OverworldSkyLightInfo? = null
        private set

    fun getLightDirection(level: Level): AbstractVec3<Float> {
        val sunAngle = level.getSunAngle(0f)
        var x = -sin(sunAngle)
        var y = cos(sunAngle)

        if (y < 0) {
            x = -x
            y = -y
        }

        return NeoVec3f(x, y, 0f)
    }

    fun getLightColor(level: Level): NeoColor {
        val y = cos(level.getSunAngle(0f))
        return NeoColor.RGBF(if (y > 0) lightInfo!!.sunColor else lightInfo!!.moonColor * y)
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
    ) : SkyLightStorage<OverworldSkyLightInfo>, NativeResource, LightFacePredicate {
        private var shadowsDirty = true
        @JvmField
        val mesh: AsyncBlockShadowMesh<*> = AsyncBlockShadowMesh(SkyLightMesher(pos)) { info ->
            LightMesh.initBlitMesh(blitMesh, info)
        }
        @JvmField
        val blitMesh = Mesh(
            BLIT_VERTEX_FORMAT,
            GlBeginMode.QUADS,
            GlBufferWriter.Mode.REGULAR,
            GlBufferUsage.STREAM_DRAW
        )

        fun updateShadows(manager: LightManager, data: RenderEventData, debugOut: (key: String, value: Int) -> Unit) {
            if (shadowsDirty) {
                mesh.rebuildAsync(manager, this) {
                    true
                }
                shadowsDirty = false
            }

            mesh.checkIfFinished()

            if (mesh.isTaskActive()) {
                debugOut("asyncTasks", 1)
            }

            /*
            mesh.lightMesh.target.bind(NeoRect2i(0, 0, mesh.lightMesh.texture.width, mesh.lightMesh.texture.height)).use { fbo ->
                fbo.clear(GlClearBit.Color(getLightColor(data.level)))
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, mesh.shadowBuffer.glId)
                blitMesh.draw()

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
             */
        }

        fun render(shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit) {
            if (lightInfo != null) {
                mesh.lightMesh.draw(shader)
                debugOut("chunksRendered", 1)
            }
        }

        override fun shouldCastBlock(
            level: Level,
            pos: AbstractVec3<Int>,
            state: BlockState
        ): Boolean {
            return level.getBrightness(LightLayer.SKY, pos.blockPos) > 0 || level.getBrightness(LightLayer.SKY, (pos + NeoDirection.UP).blockPos) > 0 || level.getBrightness(LightLayer.SKY, (pos + if (getLightDirection(level).x > 0) NeoDirection.EAST else NeoDirection.WEST).blockPos) > 0
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

            if (face == NeoDirection.DOWN) {
                return false
            }

            if (state.block is LeavesBlock && level.getBlockState((pos + face).blockPos).block is LeavesBlock) {
                return false
            }

            return BlockUtil.INSTANCE.shouldRenderFace(
                level,
                pos,
                face,
                state
            )
        }

        fun markNeighborsDirty() {
            chunks[ChunkPos(pos.x + 1, pos.z)]?.shadowsDirty = true
            chunks[ChunkPos(pos.x - 1, pos.z)]?.shadowsDirty = true
            chunks[ChunkPos(pos.x, pos.z + 1)]?.shadowsDirty = true
            chunks[ChunkPos(pos.x, pos.z - 1)]?.shadowsDirty = true
        }

        override fun load(
            manager: LightManager,
            info: OverworldSkyLightInfo
        ) {
            shadowsDirty = true
            markNeighborsDirty()
        }

        override fun reload(manager: LightManager) {
            shadowsDirty = true
        }

        override fun loadChunk(
            manager: LightManager,
            chunk: LevelChunk
        ) {
            shadowsDirty = true
            markNeighborsDirty()
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