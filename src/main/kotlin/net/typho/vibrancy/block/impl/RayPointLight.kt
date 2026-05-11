package net.typho.vibrancy.block.impl

//? if 1.21 {
import dev.ryanhcode.sable.companion.SableCompanion
//? }

//? if >=1.21.9 {
/*import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.OutlineBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.state.CameraRenderState
*///? }

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlBlendShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.util.*
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3f
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.NeoVec3d
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.big_shot_lib.api.util.BlockUtil
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
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
import net.typho.vibrancy.util.EmptyVertexConsumer
import net.typho.vibrancy.util.PointLight
import net.typho.vibrancy.util.QuadListVertexConsumer
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector4f
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
        fun drawState(shader: NeoIdentifier, uniforms: GlBoundProgram.() -> Unit) = GlDrawState.Basic(
            blend = GlBlendShard.Enabled(
                BlendFunction.Basic(
                    GlBlendingFactor.DST_COLOR,
                    GlBlendingFactor.ZERO
                ),
                GlBlendEquation.ADD
            ),
            shader = GlShaderShard.FromLocation(
                shader,
                uniforms
            )
        )
    }

    @JvmField
    val blitMesh = Mesh(
        LightMesh.BLIT_VERTEX_FORMAT,
        GlBeginMode.QUADS,
        GlBufferWriter.Mode.REGULAR,
        GlBufferUsage.STREAM_DRAW
    )

    fun blit(target: LightTexture, shadowBuffer: ShadowBuffer, uniforms: GlBoundProgram.() -> Unit, shader: NeoIdentifier) {
        target.framebuffer.bind(NeoRect2i(0, 0, target.width!!, target.height!!)).use { fbo ->
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, shadowBuffer.glId)

            drawState(shader) {
                uniforms(this)

                setUniform("LightPos") { setFloatVec(offset) }
                setUniform("LightColor") { setFloatVec(color * VibrancyConfig.rayLightBrightness) }
                setUniform("LightRadius") { set(radius) }
            }.bind().use { blitMesh.draw() }
        }
    }

    @JvmField
    val meshCollector = FloodFillBlockMeshCollector(pos.blockPos)

    var meshData: LightMesh.MeshData? = null
        protected set

    @JvmField
    val dynamicTexture = LightTexture()
    @JvmField
    val dynamicBuffer = ShadowBuffer(GlBufferUsage.STREAM_DRAW)
    @JvmField
    val dynamicBVHBuffer = NeoGlBuffer()
    @JvmField
    val dynamicTextureInfoBuffer = NeoGlBuffer()
    @JvmField
    protected var dynamicCleared = true

    @JvmField
    val staticTexture = LightTexture()
    @JvmField
    val mesh = StaticBlockLightMeshManager { mesh, info ->
        meshData = info
        staticTexture.resize(info.sections.size.x, info.sections.size.y)
        dynamicTexture.resize(info.sections.size.x, info.sections.size.y)
        LightMesh.initBlitMesh(blitMesh, info)
        staticTexture.framebuffer.bind(NeoRect2i(0, 0, staticTexture.width!!, staticTexture.height!!)).use { fbo ->
            staticTexture.clear()
            blit(
                staticTexture,
                mesh.shadowBuffer,
                {
                    setTexture(0, GlTextureBinding.FromInstance(
                        NeoAtlas.blocks,
                        GlTextureTarget.TEXTURE_2D
                    ))
                },
                Vibrancy.id("block/raytraced/blit")
            )
        }
        dynamicTexture.clear()
    }

    @JvmField
    var shadowsDirty = true

    constructor(info: RayPointLightInfo, state: BlockState, pos: IVec3<Int>) : this(
        info.color(state) * info.brightness(state),
        info.radius(state),
        info.offset(state),
        pos
    )

    //? if 1.21 {
    override val absolutePos: IVec3<Float>
        get() = SableCompanion.INSTANCE.getContainingClient((pos.toDouble() + offset.toDouble()).toJOML())?.let { NeoVec3d(it.renderPose(Vibrancy.tickDelta).transformPosition((pos.toDouble() + offset.toDouble()).toJOML())).toFloat() } ?: (pos.toFloat() + offset)
    val absoluteBlockPos: IVec3<Float>
        get() = SableCompanion.INSTANCE.getContainingClient(pos.toDouble().toJOML())?.let { NeoVec3d(it.renderPose(Vibrancy.tickDelta).transformPosition(pos.toDouble().toJOML())).toFloat() } ?: pos.toFloat()
    //? } else {
    /*override val absolutePos: IVec3<Float>
        get() = pos.toFloat() + offset
    val absoluteBlockPos: IVec3<Float>
        get() = pos.toFloat()
    *///? }
    override val boundingBox: AbstractRect3<Int>
        get() = NeoRect3i(pos - radius.toInt(), pos + radius.toInt())
    override val shadowBox: AbstractRect3<Int>
        get() {
            val shadowRadius = ceil(radius.coerceAtMost(VibrancyConfig.rayLightShadowRadius.toFloat())).toInt()
            return NeoRect3i(pos - shadowRadius, pos + shadowRadius)
        }
    val shadowPredicate = object : BlockMeshCollector.Predicate {
        override fun isBlockTransparent(level: Level, pos: BlockPos.MutableBlockPos, state: BlockState): Boolean {
            return super.isBlockTransparent(level, pos, state) || BlockLightRegistry.get(state.block, RayPointLightType) != null
        }

        override fun shouldCastBlock(
            level: Level,
            pos: BlockPos.MutableBlockPos,
            state: BlockState?
        ): Boolean {
            return shadowBox.contains(NeoVec3i(pos)) && BlockLightRegistry.get((state ?: level.getBlockState(pos)).block, RayPointLightType) == null
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

            val sidePos = pos.relative(face.mojang)

            if (sidePos == this@RayPointLight.pos) {
                return true
            }

            val state = state ?: level.getBlockState(pos)

            if (BlockUtil.INSTANCE.getBlockChunkLayer(state) == BlockChunkLayer.SOLID && !face.isPointingTowards(pos, this@RayPointLight.pos)) {
                return false
            }

            if (
                !BlockUtil.INSTANCE.shouldRenderFace(
                    level,
                    pos,
                    face,
                    state
                ) && BlockLightRegistry.get(level.getBlockState(sidePos).block, RayPointLightType) == null
            ) {
                return false
            }

            return true
        }
    }
    val lightPredicate = object : BlockMeshCollector.Predicate {
        override fun shouldCastBlock(
            level: Level,
            pos: BlockPos.MutableBlockPos,
            state: BlockState?
        ): Boolean {
            return boundingBox.contains(NeoVec3i(pos))
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

            if (BlockUtil.INSTANCE.getBlockChunkLayer(state) == BlockChunkLayer.SOLID && !face.isPointingTowards(pos, this@RayPointLight.pos)) {
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
    }

    fun reload() {
        synchronized(meshCollector) {
            meshCollector.markAllDirty()
        }
        shadowsDirty = true
    }

    override fun free() {
        dynamicTexture.free()
        dynamicBuffer.free()
        dynamicBVHBuffer.free()
        dynamicTextureInfoBuffer.free()
        staticTexture.free()
        mesh.free()
    }

    fun update(data: RenderEventData, manager: LightManager, debugOut: (String, Int) -> Unit, dynamicShadows: Boolean, profiler: ProfilerFiller) {
        profiler.push("rebuildBlocks")
        synchronized(meshCollector) {
            for (pos in manager.dirtyBlocks) {
                if (boundingBox.contains(pos)) {
                    shadowsDirty = shadowsDirty or meshCollector.markDirty(pos.blockPos)
                }
            }
        }

        if (shadowsDirty) {
            mesh.rebuildBlocksAsync(data, pos, manager, meshCollector, shadowPredicate, lightPredicate)
            shadowsDirty = false
        }
        profiler.pop()

        profiler.push("finish")
        mesh.checkIfFinished()
        profiler.pop()

        if (dynamicShadows) {
            profiler.push("dynamicShadows")
            manager.getLevel()?.let { level ->
                profiler.push("collect")

                dynamicCleared = false

                val absolutePos = absolutePos
                val absoluteBlockPos = absoluteBlockPos

                //? if 1.21 {
                val subLevel = SableCompanion.INSTANCE.getContainingClient(pos.toDouble().toJOML())
                val subLevelPose = subLevel?.renderPose()
                val transform = if (subLevelPose == null) {
                    Matrix4f().translate((-absoluteBlockPos).toJOML())
                } else {
                    Matrix4f().rotate(Quaternionf(subLevelPose.orientation()).invert()).translate((-absoluteBlockPos).toJOML())
                }
                //? } else {
                /*val transform = Matrix4f().translate((-absoluteBlockPos).toJOML())
                *///? }

                val allTextures = hashSetOf<NeoIdentifier>()

                data class Node(
                    val quads: MutableMap<NeoIdentifier, MutableList<NeoBakedQuad>> = hashMapOf(),
                    val buffers: MutableMap<NeoIdentifier, NeoBakedQuad.Consumer> = hashMapOf(),
                    val bufferSource: MultiBufferSource = WrapperUtil.INSTANCE.unwrap { settings: NeoRenderSettings ->
                        val texture = settings.drawState.shader.textures.getOrNull(0)?.location ?: return@unwrap EmptyVertexConsumer

                        if (GlTexture2D[texture] == null) {
                            return@unwrap EmptyVertexConsumer
                        }

                        if (texture.equals("minecraft", "textures/entity/beacon_beam.png")) {
                            return@unwrap EmptyVertexConsumer
                        }

                        allTextures.add(texture)

                        buffers.computeIfAbsent(texture) {
                            val quads = quads.computeIfAbsent(texture) { texture -> arrayListOf() }
                            object : QuadListVertexConsumer(quads) {
                                override fun vertex(
                                    x: Float,
                                    y: Float,
                                    z: Float
                                ): NeoVertexConsumer {
                                    val pos = transform.transform(Vector4f(x, y, z, 1f))
                                    return super.vertex(pos.x, pos.y, pos.z)
                                }
                            }
                        }
                    }
                ) {
                    fun getQuads(textures: List<NeoIdentifier>) = textures.mapIndexedNotNull { index, texture -> quads[texture]?.map { it to index } }.flatten()

                    fun computeBox(textures: List<NeoIdentifier>): AbstractRect3<Float>? {
                        var min: IVec3<Float>? = null
                        var max: IVec3<Float>? = null

                        for (texture in textures) {
                            quads[texture]?.let { builder ->
                                min = builder.fold(min) { accum: IVec3<Float>?, quad -> quad.v0.pos.min(quad.v1.pos.min(quad.v2.pos.min(if (accum == null) quad.v3.pos else quad.v3.pos.min(accum)))) }
                                max = builder.fold(max) { accum: IVec3<Float>?, quad -> quad.v0.pos.max(quad.v1.pos.max(quad.v2.pos.max(if (accum == null) quad.v3.pos else quad.v3.pos.max(accum)))) }
                            }
                        }

                        if (min != null && max != null) {
                            return NeoRect3f(min, max)
                        }

                        return null
                    }
                }

                val nodes = arrayListOf<Node>()
                val poseStack = PoseStack()

                if (VibrancyConfig.entityShadowsEnabled) {
                    profiler.push("entityShadows")
                    for (entity in level.getEntities(null, AABB.ofSize(Vec3(absolutePos.toJOML()), radius.toDouble() * 2, radius.toDouble() * 2, radius.toDouble() * 2))) {
                        //? if 1.21 {
                        if (subLevel != null || meshCollector.cache.checked.contains(entity.blockPosition())) {
                            //? } else {
                            /*if (meshCollector.checked.contains(NeoVec3i(entity.blockPosition()))) {
                            *///? }
                            val node = Node()
                            debugOut("entityShadows", 1)
                            //? if <1.21.9 {
                            Minecraft.getInstance().entityRenderDispatcher.render(
                                entity,
                                Mth.lerp(Vibrancy.tickDelta.toDouble(), entity.xOld, entity.x),
                                Mth.lerp(Vibrancy.tickDelta.toDouble(), entity.yOld, entity.y),
                                Mth.lerp(Vibrancy.tickDelta.toDouble(), entity.zOld, entity.z),
                                Mth.lerp(Vibrancy.tickDelta, entity.yRotO, entity.yRot),
                                Vibrancy.tickDelta,
                                poseStack,
                                node.bufferSource,
                                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT
                            )
                            //? } else {
                            /*val renderer = Minecraft.getInstance().entityRenderDispatcher.getRenderer(entity)

                            if (renderer.shouldRender(entity, Frustum(data.modelViewMat, data.projMat), data.camera.pos.x.toDouble(), data.camera.pos.y.toDouble(), data.camera.pos.z.toDouble())) {
                                val storage = SubmitNodeStorage()
                                val features = FeatureRenderDispatcher(
                                    storage,
                                    Minecraft.getInstance().blockRenderer,
                                    node.bufferSource,
                                    Minecraft.getInstance().atlasManager,
                                    object : OutlineBufferSource() {
                                        override fun getBuffer(renderType: RenderType): VertexConsumer {
                                            return WrapperUtil.INSTANCE.unwrap(EmptyVertexConsumer)
                                        }
                                    },
                                    WrapperUtil.INSTANCE.unwrap { EmptyVertexConsumer },
                                    Minecraft.getInstance().font
                                )

                                renderer.submit(
                                    renderer.createRenderState(entity, Vibrancy.tickDelta),
                                    poseStack,
                                    storage,
                                    Minecraft.getInstance().gameRenderer.levelRenderState.cameraRenderState
                                )

                                storage.endFrame()
                            }
                            *///? }
                            nodes.add(node)
                        }
                    }
                    profiler.pop()
                }

                if (VibrancyConfig.blockEntityShadows) {
                    profiler.push("blockEntityShadows")
                    Vibrancy.disableFlywheelInstancing = true

                    for (pos in meshCollector.blockEntities) {
                        level.getBlockEntity(pos)?.let { blockEntity ->
                            Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(blockEntity)?.let { renderer ->
                                val node = Node()
                                debugOut("blockEntityShadows", 1)

                                poseStack.pushPose()

                                //? if 1.21 {
                                if (subLevelPose == null) {
                                    poseStack.translate(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                                } else {
                                    val pos = subLevelPose.transformPosition(NeoVec3i(pos).toDouble().toJOML())
                                    poseStack.translate(pos.x, pos.y, pos.z)
                                    poseStack.mulPose(Quaternionf(subLevelPose.orientation()))
                                }
                                //? } else {
                                /*poseStack.translate(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                                *///? }

                                renderer.render(
                                    blockEntity,
                                    Vibrancy.tickDelta,
                                    poseStack,
                                    node.bufferSource,
                                    15728880,
                                    OverlayTexture.NO_OVERLAY
                                )

                                poseStack.popPose()
                                nodes.add(node)
                            }
                        }
                    }

                    Vibrancy.disableFlywheelInstancing = false
                    profiler.pop()
                }
                profiler.pop()

                profiler.push("calculate")
                dynamicTexture.framebuffer.bind(NeoRect2i(0, 0, dynamicTexture.width!!, dynamicTexture.height!!)).use { fbo ->
                    var cleared = false

                    nodes.forEach { it.buffers.forEach { (texture, consumer) -> consumer.flush() } }

                    val chunkedTextures = allTextures.chunked(8)

                    for (textures in chunkedTextures) {
                        val nodes = nodes.mapNotNull { node -> node.computeBox(textures)?.let { node to it } }

                        if (nodes.isNotEmpty()) {
                            val bvhBuffer = NeoBuffer.Native(nodes.size * 32L)

                            bvhBuffer.write().run {
                                var index = 0

                                for (node in nodes) {
                                    writeFloat(node.second.min.x)
                                    writeFloat(node.second.min.y)
                                    writeFloat(node.second.min.z)
                                    writeInt(index)

                                    writeFloat(node.second.max.x)
                                    writeFloat(node.second.max.y)
                                    writeFloat(node.second.max.z)
                                    index += node.first.getQuads(textures).size
                                    writeInt(index)
                                }
                            }

                            dynamicBVHBuffer.bind(GlBufferTarget.ARRAY_BUFFER).use {
                                it.bufferData(bvhBuffer, GlBufferUsage.STREAM_DRAW)
                            }
                            bvhBuffer.free()

                            if (!cleared) {
                                dynamicTexture.clear()
                                cleared = true
                            }

                            val quads = nodes.flatMap { it.first.getQuads(textures) }
                            val textures = textures.map { GlTexture2D[it]!! }

                            val texBuffer = NeoBuffer.Native(quads.size * 4L)

                            texBuffer.write().run {
                                for (quad in quads) {
                                    writeInt(quad.second)
                                }
                            }

                            dynamicTextureInfoBuffer.bind(GlBufferTarget.ARRAY_BUFFER).use {
                                it.bufferData(texBuffer, GlBufferUsage.STREAM_DRAW)
                            }
                            texBuffer.free()

                            dynamicBuffer.lazyUploadQuads(textures, quads)()
                            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, dynamicBVHBuffer.glId)
                            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, dynamicTextureInfoBuffer.glId)
                            blit(
                                dynamicTexture,
                                dynamicBuffer,
                                {
                                    setTextureArray(0, "Samplers", *textures.map { GlTextureBinding.FromInstance(it, GlTextureTarget.TEXTURE_2D) }.toTypedArray())
                                },
                                Vibrancy.id("block/raytraced/dynamic_blit")
                            )
                        }
                    }
                }
                profiler.pop()

                debugOut("lightsWithEntityShadows", 1)
            }
            profiler.pop()
        } else {
            if (!dynamicCleared) {
                dynamicTexture.clear()
                dynamicCleared = true
            }
        }
    }

    fun render(data: RenderEventData, shader: GlBoundProgram, debugOut: (key: String, value: Int) -> Unit, profiler: ProfilerFiller) {
        debugOut("lightsRendered", 1)

        if (mesh.isTaskActive()) {
            debugOut("numAsyncTasks", 1)
        }

        profiler.push("transforms")
        //? if 1.21 {
        val subLevel = SableCompanion.INSTANCE.getContainingClient(pos.toDouble().toJOML())

        if (subLevel == null) {
            shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((pos.toFloat() - data.camera.pos).toJOML(), Matrix4f())) }
            shader.setUniform("SpecularMat") { set(Matrix4f()) }
            shader.setUniform("CameraPos") { setFloatVec(data.camera.pos - pos.toFloat()) }
        } else {
            val pose = subLevel.renderPose(Vibrancy.tickDelta)
            val orientation = Quaternionf(pose.orientation())
            val pos = NeoVec3d(pose.transformPosition(pos.toDouble().toJOML()))
            shader.setUniform("ModelViewMat") {
                set(
                    data.modelViewMat
                        .translate((pos - data.camera.pos.toDouble()).toFloat().toJOML(), Matrix4f())
                        .rotate(orientation)
                )
            }
            shader.setUniform("SpecularMat") {
                set(
                    Matrix4f()
                        .rotate(orientation)
                )
            }
            shader.setUniform("CameraPos") { setFloatVec(data.camera.pos - pos.toFloat()) }
        }
        //? } else {
        /*shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((pos.toFloat() - data.camera.pos).toJOML(), Matrix4f())) }
        shader.setUniform("SpecularMat") { set(Matrix4f()) }
        shader.setUniform("CameraPos") { setFloatVec(data.camera.pos - pos.toFloat()) }
        *///? }
        profiler.pop()

        profiler.push("uniforms")
        shader.setUniform("LightPos") { setFloatVec(offset) }
        shader.setUniform("LightColor") { setFloatVec(color) }
        shader.setUniform("LightRadius") { set(radius) }
        shader.setTexture(0, GlTextureBinding.FromInstance(
            NeoAtlas.blocks,
            GlTextureTarget.TEXTURE_2D
        ))
        shader.setTexture(1, GlTextureBinding.FromInstance(
            staticTexture,
            GlTextureTarget.TEXTURE_2D
        ))
        shader.setTexture(2, GlTextureBinding.FromInstance(
            dynamicTexture,
            GlTextureTarget.TEXTURE_2D
        ))
        profiler.pop()

        profiler.push("draw")
        mesh.lightMesh.draw()
        profiler.pop()
    }
}