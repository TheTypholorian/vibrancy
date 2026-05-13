package net.typho.vibrancy.sky.impl

//? if 1.21 {
/*import dev.ryanhcode.sable.companion.SableCompanion
*///? }

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.ARGB
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
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlBlendShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDepthShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.util.BlockChunkLayer
import net.typho.big_shot_lib.api.client.rendering.util.FogUtil
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
import net.typho.big_shot_lib.api.math.vec.NeoVec3d
import net.typho.big_shot_lib.api.math.vec.NeoVec3f
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
import net.typho.vibrancy.util.EntityRenderingUtil
import net.typho.vibrancy.util.GlTask
import net.typho.vibrancy.util.QuadListVertexConsumer
import net.typho.vibrancy.util.ReflectionAtlases
import net.typho.vibrancy.util.VibrancyThreadPool
import org.joml.FrustumIntersection
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector4f
import org.lwjgl.system.NativeResource
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class OverworldSkyLightStorage : ChunkedSkyLightStorage<OverworldSkyLightInfo, OverworldSkyLightStorage.Chunk>(OverworldSkyLightType) {
    companion object {
        @JvmField
        val shadowDrawState = GlDrawState.Basic(
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
        val entityShadowDrawState = GlDrawState.Basic(
            depth = GlDepthShard.Enabled(
                GlAlphaFunction.GEQUAL
            ),
            shader = GlShaderShard.FromLocation(
                Vibrancy.id("sky/overworld/blit_entity_solid"),
                { },
                GlTextureBinding.FromInstance(
                    NeoAtlas.blocks,
                    GlTextureTarget.TEXTURE_2D
                )
            )
        )
        @JvmField
        val entityTranslucentShadowDrawState = GlDrawState.Basic(
            depth = GlDepthShard.Enabled(
                GlAlphaFunction.GEQUAL
            ),
            shader = GlShaderShard.FromLocation(
                Vibrancy.id("sky/overworld/blit_entity_translucent"),
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

    override fun loadChunk(manager: LightManager, chunk: ChunkAccess) {
        for (x in (chunk.pos.x - 1)..(chunk.pos.x + 1)) {
            for (z in (chunk.pos.z - 1)..(chunk.pos.z + 1)) {
                getOrCreateChunk(manager, ChunkPos(x, z)).loadChunk(manager, manager.getLevel()!!.getChunk(x, z))
            }
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    fun render(data: RenderEventData, manager: LightManager, result: GlFramebuffer, temp: GlFramebuffer, debugOut: (key: String, value: Int) -> Unit, profiler: ProfilerFiller) {
        if (!VibrancyConfig.skyLightsEnabled) {
            return
        }

        debugOut("numAsyncTasks", chunks.values.count { it.isTaskActive() })
        debugOut("numChunks", chunks.size)

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
            //? if <1.21.5 {
            /*val sunriseColor = data.level!!.effects().getSunriseColor(data.level!!.getTimeOfDay(Vibrancy.tickDelta), Vibrancy.tickDelta)

            if (sunriseColor != null) {
                lightColor = lightColor.lerp(sunriseColor[0], sunriseColor[1], sunriseColor[2], sunriseColor[3])
            }
            *///? } else {
            val timeOfDay = data.level!!.getTimeOfDay(Vibrancy.tickDelta)

            if (data.level!!.effects().isSunriseOrSunset(timeOfDay)) {
                val sunriseColor = NeoColor.argbF(data.level!!.effects().getSunriseOrSunsetColor(timeOfDay))
                lightColor = lightColor.lerp(sunriseColor.redF, sunriseColor.greenF, sunriseColor.blueF, sunriseColor.alphaF)
            }
            //? }
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
            .scale(1f / (manager.clampToChunkRenderDistance(VibrancyConfig.skyLightShadowDistance) * 16))
            .rotate(shadowRot)
        val shadowFrustum = FrustumIntersection(shadowMat)
        profiler.pop()

        profiler.push("shadows")
        shadowDrawState.bind().use { settings ->
            settings.shader.setUniform("ShadowMat") { set(shadowMat) }
            settings.shader.setUniform("CameraPos") { setFloatVec(data.camera.pos) }

            fun drawChunks(name: String, to: LightTexture, mesh: (chunk: Chunk) -> Mesh) {
                profiler.push(name)
                to.framebuffer.bind(NeoRect2i(0, 0, to.width!!, to.height!!)).use { fbo ->
                    profiler.push("clear")
                    to.clear()
                    profiler.pop()

                    profiler.push("chunks")
                    for ((pos, chunk) in chunks) {
                        val mesh = mesh(chunk)

                        if (mesh.size > 0) {
                            profiler.push("transforms")
                            val blockPos = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)

                            //? if 1.21 {
                            /*val subLevel = SableCompanion.INSTANCE.getContainingClient(pos)

                            if (subLevel == null) {
                                if (chunk.shouldDraw(shadowFrustum, data)) {
                                    settings.shader.setUniform("ChunkOffset") { setFloatVec(blockPos.toFloat()) }
                                    mesh(chunk).draw()
                                }
                            } else {
                                val pose = subLevel.renderPose(Vibrancy.tickDelta)
                                val orientation = Quaternionf(pose.orientation())
                                val origin = NeoVec3d(pose.transformPosition(blockPos.toDouble().toJOML())).toFloat()
                                settings.shader.setUniform("ShadowMat") {
                                    set(
                                        Matrix4f()
                                            .rotate(orientation)
                                            .mul(shadowMat)
                                    )
                                }
                                settings.shader.setUniform("ChunkOffset") { setFloatVec(origin) }
                                mesh(chunk).draw()
                                settings.shader.setUniform("ShadowMat") { set(shadowMat) }
                            }
                            *///? } else {
                            if (chunk.shouldDraw(shadowFrustum, data)) {
                                settings.shader.setUniform("ChunkOffset") { setFloatVec(blockPos.toFloat()) }
                                mesh(chunk).draw()
                            }
                            //? }

                            profiler.pop()
                        }
                    }
                    profiler.pop()
                }
                profiler.pop()
            }

            drawChunks("solid", texture, Chunk::mesh)

            if (VibrancyConfig.skyLightTranslucentEnabled) {
                drawChunks("translucent", translucent, Chunk::translucentMesh)
            } else {
                translucent.clear()
            }

            profiler.push("dynamicShadows")
            val quads = hashMapOf<Pair<Boolean, NeoIdentifier>, MutableList<NeoBakedQuad>>()
            val buffers = hashMapOf<NeoIdentifier, NeoBakedQuad.Consumer>()
            val bufferSource = WrapperUtil.INSTANCE.unwrap { settings: NeoRenderSettings ->
                val texture = settings.drawState.shader.textures.getOrNull(0)?.location ?: return@unwrap EmptyVertexConsumer

                if (GlTexture2D[texture] == null) {
                    return@unwrap EmptyVertexConsumer
                }

                if (texture.equals("minecraft", "textures/entity/beacon_beam.png")) {
                    return@unwrap EmptyVertexConsumer
                }

                buffers.computeIfAbsent(texture) {
                    QuadListVertexConsumer(quads.computeIfAbsent((settings.drawState.blend is GlBlendShard.Enabled) to texture) { texture -> arrayListOf() })
                }
            }
            val poseStack = PoseStack()
            val radius = VibrancyConfig.entityShadowDistance * 16

            profiler.push("collect")
            if (VibrancyConfig.entityShadowsEnabled) {
                profiler.push("entityShadows")
                for (entity in data.level!!.getEntities(null, AABB.ofSize(Vec3(data.camera.pos.toJOML()), radius.toDouble() * 2, radius.toDouble() * 2, radius.toDouble() * 2))) {
                    EntityRenderingUtil.render(entity, poseStack, bufferSource)
                }
                profiler.pop()
            }

            if (VibrancyConfig.blockEntityShadows) {
                profiler.push("blockEntityShadows")
                Vibrancy.disableFlywheelInstancing = true

                for ((pos, chunk) in chunks) {
                    for (blockPos in chunk.blockEntities) {
                        //? if 1.21 {
                        /*val subLevel = SableCompanion.INSTANCE.getContainingClient(pos)
                        val subLevelPose = subLevel?.renderPose()
                        val transformedPos = subLevelPose?.transformPosition(NeoVec3i(blockPos).toDouble().toJOML())?.let { NeoVec3d(it).toFloat() } ?: NeoVec3i(blockPos).toFloat()

                        if (transformedPos.inDistance(data.camera.pos, radius.toFloat())) {
                        *///? } else {
                        val transformedPos = NeoVec3i(blockPos).toFloat()

                        if (transformedPos.inDistance(data.camera.pos, radius.toFloat())) {
                        //? }
                            data.level!!.getBlockEntity(blockPos)?.let { blockEntity ->
                                poseStack.pushPose()
                                poseStack.translate(transformedPos.x, transformedPos.y, transformedPos.z)

                                //? if 1.21 {
                                /*if (subLevelPose != null) {
                                    poseStack.mulPose(Quaternionf(subLevelPose.orientation()))
                                }
                                *///? }

                                EntityRenderingUtil.renderBlockEntity(blockEntity, poseStack, bufferSource, data)

                                poseStack.popPose()
                            }
                        }
                    }
                }

                Vibrancy.disableFlywheelInstancing = false
                profiler.pop()
            }
            profiler.pop()

            profiler.push("calculate")
            for ((key, quads) in quads) {
                if (quads.isNotEmpty()) {
                    GlTexture2D[key.second]?.let { texture ->
                        tempMesh.lazyUploadQuadsNoAtlas(quads)()

                        fun draw(state: GlDrawState, to: LightTexture) {
                            state.bind().use { settings ->
                                settings.shader.setUniform("ShadowMat") { set(shadowMat) }
                                settings.shader.setUniform("CameraPos") { setFloatVec(data.camera.pos) }
                                settings.shader.setUniform("ChunkOffset") { setFloatVec(NeoVec3f(0f, 0f, 0f)) }
                                settings.shader.setTexture(
                                    0,
                                    GlTextureBinding.FromInstance(
                                        texture,
                                        GlTextureTarget.TEXTURE_2D
                                    )
                                )

                                to.framebuffer.bind(NeoRect2i(0, 0, to.width!!, to.height!!)).use { fbo ->
                                    tempMesh.draw()
                                }
                            }
                        }

                        draw(entityShadowDrawState, this.texture)

                        if (key.first) {
                            draw(entityTranslucentShadowDrawState, translucent)
                        }
                    }
                }
            }
            profiler.pop()
            profiler.pop()
        }
        profiler.pop()

        profiler.push("draw")
        temp.bind().use { fbo ->
            profiler.push("clear")
            fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))
            profiler.pop()

            profiler.push("draw")
            lightDrawState.bind().use { settings ->
                settings.shader.setUniform("ModelViewMat") { set(Matrix4f().translate((-data.camera.pos).toJOML())) }
                settings.shader.setUniform("ProjMat") { set(data.projMat) }
                settings.shader.setUniform("ShadowMat") { set(shadowMat) }
                FogUtil.INSTANCE.upload(settings.shader)
                settings.shader.setUniform("CameraPos") { setFloatVec(data.camera.pos) }
                settings.shader.setUniform("LightColor") { setFloatVec(lightColor) }
                settings.shader.setUniform("LightDirection") { setFloatVec(NeoVec4f(shadowRot.invert(Quaternionf()).transform(Vector4f(0f, 0f, 1f, 0f))).xyz) }

                settings.shader.setUniform("ShadowMapPower") { set(VibrancyConfig.skyLightShadowMapPower) }
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
                    if (chunk.mesh.size > 0 || chunk.translucentMesh.size > 0) {
                        //if (chunk.box == null || data.frustum.testAab((chunk.box!!.min.toFloat() - data.camera.pos).toJOML(), (chunk.box!!.min.toFloat() + 1f - data.camera.pos).toJOML())) {
                        profiler.push("transforms")
                        val blockPos = NeoVec3i(pos.minBlockX, 0, pos.minBlockZ)

                        //? if 1.21 {
                        /*val subLevel = SableCompanion.INSTANCE.getContainingClient(pos)

                        if (subLevel == null) {
                            settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((blockPos.toFloat() - data.camera.pos).toJOML(), Matrix4f())) }
                            settings.shader.setUniform("SableMat") {
                                set(
                                    Matrix4f()
                                        .translate((blockPos.toFloat() - data.camera.pos).toFloat().toJOML())
                                )
                            }
                            settings.shader.setUniform("SpecularMat") {
                                set(
                                    Matrix4f()
                                        .translate(blockPos.toFloat().toJOML())
                                )
                            }
                        } else {
                            val pose = subLevel.renderPose(Vibrancy.tickDelta)
                            val orientation = Quaternionf(pose.orientation())
                            val pos = NeoVec3d(pose.transformPosition(blockPos.toDouble().toJOML()))
                            settings.shader.setUniform("ModelViewMat") {
                                set(
                                    data.modelViewMat
                                        .translate((pos.toFloat() - data.camera.pos).toJOML(), Matrix4f())
                                        .rotate(orientation)
                                )
                            }
                            settings.shader.setUniform("SableMat") {
                                set(
                                    Matrix4f()
                                        .translate((pos.toFloat() - data.camera.pos).toJOML())
                                        .rotate(orientation)
                                )
                            }
                            settings.shader.setUniform("SpecularMat") {
                                set(
                                    Matrix4f()
                                        .translate(pos.toFloat().toJOML())
                                        .rotate(orientation)
                                )
                            }
                        }
                        *///? } else {
                        settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((blockPos.toFloat() - data.camera.pos).toJOML(), Matrix4f())) }
                        settings.shader.setUniform("SableMat") {
                            set(
                                Matrix4f()
                                    .translate((blockPos.toFloat() - data.camera.pos).toFloat().toJOML())
                            )
                        }
                        settings.shader.setUniform("SpecularMat") {
                            set(
                                Matrix4f()
                                    .translate(blockPos.toFloat().toJOML())
                            )
                        }
                        //? }
                        profiler.pop()

                        chunk.mesh.draw()
                        chunk.translucentMesh.draw()
                        //}
                    }
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
        var blockEntities: MutableSet<BlockPos> = hashSetOf()
            private set
        private var dirty = true
        private var asyncTask: GlTask<AbstractRect3<Int>?>? = null

        override fun free() {
            mesh.free()
            asyncTask?.cancel()
        }

        fun shouldDraw(shadowFrustum: FrustumIntersection, data: RenderEventData) = box == null || shadowFrustum.testAab((box!!.min.toFloat() - data.camera.pos).toJOML(), (box!!.max.toFloat() - data.camera.pos).toJOML())

        fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

        fun checkIfFinished(): Boolean {
            asyncTask?.let { task ->
                if (task.isDoneOrCancelled()) {
                    try {
                        box = task.finish()
                    } catch (e: NullPointerException) {
                        Vibrancy.LOGGER.warn("Error finishing sky light task at $pos", e)
                    }

                    asyncTask = null
                    return true
                }
            }

            return false
        }

        private fun rebuildBlocksAsyncImpl(
            isCancelled: () -> Boolean,
            manager: LightManager
        ): Pair<AutoCloseable, () -> AbstractRect3<Int>?> {
            val level = manager.getLevel() ?: throw NullPointerException("No level?")
            var box: AbstractRect3<Int>? = null

            val lightFaces = arrayListOf<LightFace>()
            val translucentFaces = arrayListOf<LightFace>()
            val mesher = SkyLightBlockMeshCollector(pos)
            if (!mesher.submit(
                    isCancelled,
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
                )) {
                return AutoCloseable { } to { null }
            }
            blockEntities = mesher.blockEntities

            fun upload(faces: List<LightFace>, mesh: Mesh): Pair<AutoCloseable, () -> Unit> {
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

                return AutoCloseable {
                    vertexBuffer.free()
                    indices.first.free()
                } to {
                    mesh.rawUpload(faces.size * 6, indices.second, vertexBuffer, indices.first)
                }
            }

            val solid = upload(lightFaces, mesh)
            val translucent = upload(translucentFaces, translucentMesh)

            return AutoCloseable {
                solid.first.close()
                translucent.first.close()
            } to {
                solid.second()
                translucent.second()
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

                dirty = false

                if (VibrancyConfig.useMultithreading) {
                    asyncTask?.cancel()
                    asyncTask = VibrancyThreadPool.submit(data, pos, manager) { rebuildBlocksAsyncImpl(it, manager) }
                } else {
                    val result = rebuildBlocksAsyncImpl({ false }, manager)
                    box = result.second()
                    result.first.close()
                }
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
            free()
        }
    }
}