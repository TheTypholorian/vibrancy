package net.typho.vibrancy.sky.impl

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlAlphaFunction
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlClearBit
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlCullFace
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlCullShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDepthShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.util.BlockChunkLayer
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoRenderSettings
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.math.vec.NeoVec4f
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
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
import net.typho.vibrancy.util.EmptyVertexConsumer
import net.typho.vibrancy.util.QuadListVertexConsumer
import net.typho.vibrancy.util.ReflectionAtlases
import net.typho.vibrancy.util.VibrancyThreadPool
import org.joml.FrustumIntersection
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector4f
import org.lwjgl.system.NativeResource
import java.util.concurrent.CompletableFuture
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
    val texture = LightTexture.Shadow().also { it.resize(1 shl (VibrancyConfig.skyLightResolution + 10), 1 shl (VibrancyConfig.skyLightResolution + 10)) }
    @JvmField
    val translucent = LightTexture.ColorShadow().also { it.resize(1 shl (VibrancyConfig.skyLightResolution + 10), 1 shl (VibrancyConfig.skyLightResolution + 10)) }
    @JvmField
    val tempMesh = LightMesh(GlBufferUsage.STREAM_DRAW)

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
    fun render(data: RenderEventData, manager: LightManager, result: GlFramebuffer, temp: GlFramebuffer, profiler: ProfilerFiller) {
        if (!VibrancyConfig.skyLightsEnabled) {
            return
        }

        profiler.push("update")
        for ((pos, chunk) in chunks) {
            chunk.update(data, manager)
            chunk.checkIfFinished()
        }
        profiler.pop()

        profiler.push("prep")
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

        lightColor *= sqrt(sin(lightAngle).coerceAtLeast(0f))
        lightColor *= (1 - data.level!!.getRainLevel(Vibrancy.tickDelta) * 0.75f - data.level!!.getThunderLevel(Vibrancy.tickDelta) * 0.75f).coerceAtLeast(0f).coerceAtMost(1f)
        lightColor *= info!!.brightness
        lightColor *= VibrancyConfig.skyLightBrightness

        val shadowRot = Quaternionf()
            .rotateX(lightAngle)
            .rotateY(-PI.toFloat() / 2)
            .rotateY(Math.toRadians(15.0).toFloat())
        val shadowMat = Matrix4f()
            .scale(1f / (VibrancyConfig.skyLightShadowDistance * 16))
            .rotate(shadowRot)
        val shadowFrustum = FrustumIntersection(shadowMat)
        profiler.pop()

        profiler.push("shadows")
        shadowDrawState.bind().use { settings ->
            settings.shader.setUniform("ShadowMat") { set(shadowMat) }
            settings.shader.setUniform("CameraPos") { setFloatVec(data.camera.pos) }

            profiler.push("translucent")
            translucent.framebuffer.bind(NeoRect2i(0, 0, translucent.width!!, translucent.height!!)).use { fbo ->
                profiler.push("clear")
                translucent.clear()
                profiler.pop()

                profiler.push("chunks")
                for ((pos, chunk) in chunks) {
                    if (chunk.box == null || shadowFrustum.testAab((chunk.box!!.min.toFloat() - data.camera.pos).toJOML(), (chunk.box!!.max.toFloat() - data.camera.pos).toJOML())) {
                        chunk.translucentMesh.draw()
                    }
                }
                profiler.pop()
            }
            profiler.pop()

            texture.framebuffer.bind(NeoRect2i(0, 0, texture.width!!, texture.height!!)).use { fbo ->
                profiler.push("clear")
                texture.clear()
                profiler.pop()

                profiler.push("chunks")
                for ((pos, chunk) in chunks) {
                    if (chunk.box == null || shadowFrustum.testAab((chunk.box!!.min.toFloat() - data.camera.pos).toJOML(), (chunk.box!!.max.toFloat() - data.camera.pos).toJOML())) {
                        chunk.mesh.draw()
                    }
                }
                profiler.pop()

                profiler.push("dynamicShadows")
                val quads = hashMapOf<NeoIdentifier, MutableList<NeoBakedQuad>>()
                val buffers = hashMapOf<NeoIdentifier, NeoBakedQuad.Consumer>()
                val bufferSource = WrapperUtil.INSTANCE.unwrap { settings: NeoRenderSettings ->
                    val texture = settings.drawState.shader.textures.getOrNull(0)?.location ?: return@unwrap EmptyVertexConsumer

                    if (GlTexture2D[texture] == null) {
                        return@unwrap EmptyVertexConsumer
                    }

                    buffers.computeIfAbsent(texture) {
                        QuadListVertexConsumer(quads.computeIfAbsent(texture) { texture -> arrayListOf() })
                    }
                }
                val poseStack = PoseStack()
                val radius = VibrancyConfig.entityShadowDistance * 16

                profiler.push("collect")
                if (VibrancyConfig.entityShadowsEnabled) {
                    profiler.push("entityShadows")
                    for (entity in data.level!!.getEntities(null, AABB.ofSize(Vec3(data.camera.pos.toJOML()), radius.toDouble() * 2, radius.toDouble() * 2, radius.toDouble() * 2))) {
                        Minecraft.getInstance().entityRenderDispatcher.render(
                            entity,
                            Mth.lerp(Vibrancy.tickDelta.toDouble(), entity.xOld, entity.x),
                            Mth.lerp(Vibrancy.tickDelta.toDouble(), entity.yOld, entity.y),
                            Mth.lerp(Vibrancy.tickDelta.toDouble(), entity.zOld, entity.z),
                            Mth.lerp(Vibrancy.tickDelta, entity.yRotO, entity.yRot),
                            Vibrancy.tickDelta,
                            poseStack,
                            bufferSource,
                            net.minecraft.client.renderer.LightTexture.FULL_BRIGHT
                        )
                    }
                    profiler.pop()
                }

                if (VibrancyConfig.blockEntityShadows) {
                    profiler.push("blockEntityShadows")
                    Vibrancy.disableFlywheelInstancing = true

                    val origin = data.camera.pos.toInt()
                    val minChunk = ChunkPos((origin - radius).blockPos)
                    val maxChunk = ChunkPos((origin + radius).blockPos)

                    for (x in minChunk.x..maxChunk.x) {
                        for (z in minChunk.z..maxChunk.z) {
                            for ((pos, blockEntity) in data.level!!.getChunk(x, z).blockEntities) {
                                if ((NeoVec3i(pos).toFloat() + 0.5f).inDistance(data.camera.pos, radius.toFloat())) {
                                    Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(blockEntity)?.let { renderer ->
                                        poseStack.pushPose()
                                        poseStack.translate(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())

                                        renderer.render(
                                            blockEntity,
                                            Vibrancy.tickDelta,
                                            poseStack,
                                            bufferSource,
                                            15728880,
                                            OverlayTexture.NO_OVERLAY
                                        )

                                        poseStack.popPose()
                                    }
                                }
                            }
                        }
                    }

                    Vibrancy.disableFlywheelInstancing = false
                    profiler.pop()
                }
                profiler.pop()

                profiler.push("calculate")
                for ((texture, quads) in quads) {
                    if (quads.isNotEmpty()) {
                        GlTexture2D[texture]?.let { texture ->
                            settings.shader.setTexture(
                                0,
                                GlTextureBinding.FromInstance(
                                    texture,
                                    GlTextureTarget.TEXTURE_2D
                                )
                            )
                            tempMesh.lazyUploadQuadsNoAtlas(quads)()
                            tempMesh.draw()
                        }
                    }
                }
                profiler.pop()
                profiler.pop()
            }
        }
        profiler.pop()

        profiler.push("draw")
        temp.bind().use { fbo ->
            profiler.push("clear")
            fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))
            profiler.pop()

            profiler.push("draw")
            lightDrawState.bind().use { settings ->
                settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((-data.camera.pos).toJOML(), Matrix4f())) }
                settings.shader.setUniform("ProjMat") { set(data.projMat) }
                settings.shader.setUniform("ShadowMat") { set(shadowMat) }

                settings.shader.setUniform("FogStart") { set(RenderSystem.getShaderFogStart()) }
                settings.shader.setUniform("FogEnd") { set(RenderSystem.getShaderFogEnd()) }
                settings.shader.setUniform("FogShape") { set(RenderSystem.getShaderFogShape().index) }

                settings.shader.setUniform("CameraPos") { setFloatVec(data.camera.pos) }
                settings.shader.setUniform("LightColor") { setFloatVec(lightColor) }
                settings.shader.setUniform("LightDirection") { setFloatVec(NeoVec4f(shadowRot.invert(Quaternionf()).transform(Vector4f(0f, 0f, 1f, 0f))).xyz) }

                settings.shader.setUniform("ShadowBias") { set(2e-4f * (1 shl (4 - VibrancyConfig.skyLightResolution))) }

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
                        translucent,
                        GlTextureTarget.TEXTURE_2D
                    )
                )
                settings.shader.setTexture(
                    3,
                    GlTextureBinding.FromInstance(
                        translucent.depth,
                        GlTextureTarget.TEXTURE_2D
                    )
                )
                settings.shader.setTexture(4, GlTextureBinding.FromInstance(
                    ReflectionAtlases[NeoIdentifier("blocks")], //NeoAtlas.blocks.location
                    GlTextureTarget.TEXTURE_2D
                ))

                for ((pos, chunk) in chunks) {
                    //if (chunk.box == null || data.frustum.testAab((chunk.box!!.min.toFloat() - data.camera.pos).toJOML(), (chunk.box!!.min.toFloat() + 1f - data.camera.pos).toJOML())) {
                        chunk.mesh.draw()
                        chunk.translucentMesh.draw()
                    //}
                }
            }
            profiler.pop()
        }
        profiler.pop()

        profiler.push("blit")
        manager.blitFromTemp(result, temp)
        profiler.pop()
    }

    class Chunk(
        @JvmField
        val pos: ChunkPos
    ) : SkyLightStorage<OverworldSkyLightInfo>, NativeResource {
        @JvmField
        val mesh = Mesh(
            LightMesh.SKY_VERTEX_FORMAT,
            GlBeginMode.QUADS,
            GlBufferWriter.Mode.REGULAR,
            GlBufferUsage.STATIC_DRAW
        )
        @JvmField
        val translucentMesh = Mesh(
            LightMesh.SKY_VERTEX_FORMAT,
            GlBeginMode.QUADS,
            GlBufferWriter.Mode.REGULAR,
            GlBufferUsage.STATIC_DRAW
        )
        var box: AbstractRect3<Int>? = null
            private set
        private var dirty = true
        private var asyncTask: CompletableFuture<() -> AbstractRect3<Int>?>? = null

        override fun free() {
            mesh.free()
            asyncTask?.cancel(true)
        }

        fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

        fun checkIfFinished(): Boolean {
            asyncTask?.let { task ->
                try {
                    if (task.isDone) {
                        box = task.get()()
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
        ): () -> AbstractRect3<Int>? {
            val level = manager.getLevel() ?: throw NullPointerException("No level?")
            var box: AbstractRect3<Int>? = null

            val lightFaces = arrayListOf<LightFace>()
            val translucentFaces = arrayListOf<LightFace>()
            SkyLightBlockMeshCollector(pos).submit(
                manager,
                level,
                NeoAtlas.blocks,
                object : BlockMeshCollector.Consumer {
                    override val predicate: BlockMeshCollector.Predicate = object : BlockMeshCollector.Predicate {
                        override fun shouldCastBlock(
                            level: Level,
                            pos: BlockPos.MutableBlockPos,
                            state: BlockState?
                        ): Boolean {
                            val passed = Direction.entries.any {
                                val v = level.getBrightness(LightLayer.SKY, pos.move(it)) > 0
                                pos.move(it.opposite)
                                v
                            }

                            if (passed) {
                                val pos1 = NeoVec3i(pos)
                                box = box?.include(pos1) ?: NeoRect3i(pos1, pos1)
                            }

                            return passed
                        }

                        override fun shouldCastFace(
                            face: NeoDirection?,
                            level: Level,
                            pos: BlockPos.MutableBlockPos,
                            state: BlockState?
                        ): Boolean {
                            if (face == null) {
                                return true
                            }

                            val state = state ?: level.getBlockState(pos)

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

                    override fun collect(faces: Iterable<LightFace>, origin: BlockMeshCollector.FaceOrigin) {
                        if (origin is BlockMeshCollector.FaceOrigin.Block) {
                            if (BlockUtil.INSTANCE.getBlockChunkLayer(origin.block) == BlockChunkLayer.TRANSLUCENT) {
                                translucentFaces.addAll(faces)
                            } else {
                                lightFaces.addAll(faces)
                            }
                        } else if (origin is BlockMeshCollector.FaceOrigin.Fluid) {
                            if (origin.fluid.isSourceOfType(Fluids.WATER)) {
                                translucentFaces.addAll(faces)
                            } else {
                                lightFaces.addAll(faces)
                            }
                        } else {
                            lightFaces.addAll(faces)
                        }
                    }
                }
            )

            fun upload(faces: List<LightFace>, mesh: Mesh): () -> Unit {
                val vertexBuffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * LightMesh.SKY_VERTEX_FORMAT.vertexSizeBytes)

                vertexBuffer.write().run {
                    faces.forEachIndexed { index, face ->
                        for (vertex in face.quad.vertices) {
                            writeFloat(vertex.pos.x)
                            writeFloat(vertex.pos.y)
                            writeFloat(vertex.pos.z)
                            writeFloat(vertex.textureUV!!.x)
                            writeFloat(vertex.textureUV!!.y)

                            if (face.blockPos == null || face.quad.direction == null) {
                                writeInt(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                            } else {
                                val pos = (face.blockPos + face.quad.direction!!).blockPos
                                writeInt(net.minecraft.client.renderer.LightTexture.pack(
                                    level.getBrightness(LightLayer.BLOCK, pos),
                                    level.getBrightness(LightLayer.SKY, pos)
                                ))
                            }

                            writeInt(vertex.color!!.toRGBA())
                            writeByte((vertex.normal!!.x * 127).toInt())
                            writeByte((vertex.normal!!.y * 127).toInt())
                            writeByte((vertex.normal!!.z * 127).toInt())
                        }
                    }
                }

                val indices = mesh.generateIndices(faces.size * 4)

                return {
                    mesh.rawUpload(faces.size * 6, indices.second, vertexBuffer, indices.first)
                    vertexBuffer.free()
                    indices.first.free()
                }
            }

            val solid = upload(lightFaces, mesh)
            val translucent = upload(translucentFaces, translucentMesh)

            return {
                solid()
                translucent()
                box
            }
        }

        fun update(data: RenderEventData, manager: LightManager) {
            for (pos in manager.dirtyBlocks) {
                if (ChunkPos(pos.blockPos) == this.pos) {
                    dirty = true
                    break
                }
            }

            if (dirty) {
                for (x in (pos.x - 1)..(pos.x + 1)) {
                    for (z in (pos.z - 1)..(pos.z + 1)) {
                        if (!data.level!!.hasChunk(x, z)) {
                            return
                        }
                    }
                }

                if (VibrancyConfig.useMultithreading) {
                    asyncTask?.cancel(true)
                    asyncTask = VibrancyThreadPool.submit(data, pos, manager) { rebuildBlocksAsyncImpl(manager) }
                } else {
                    rebuildBlocksAsyncImpl(manager)()
                }
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