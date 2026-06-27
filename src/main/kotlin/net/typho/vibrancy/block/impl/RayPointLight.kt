package net.typho.vibrancy.block.impl

//? if 1.21 {
import dev.ryanhcode.sable.companion.SableCompanion
//? }

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.SectionPos
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlBlendShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.util.*
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3f
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.math.vec.NeoVec3d
import net.typho.big_shot_lib.api.math.vec.NeoVec3i
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.shadows.DynamicLightFace
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.shadows.LightTexture
import net.typho.vibrancy.shadows.ShadowBuffer
import net.typho.vibrancy.shadows.StaticOneStepBlockLightMeshManager
import net.typho.vibrancy.shadows.VoxelGridBuffer
import net.typho.vibrancy.util.EmptyVertexConsumer
import net.typho.vibrancy.util.EntityRenderingUtil
import net.typho.vibrancy.util.PointLight
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.system.NativeResource
import kotlin.math.ceil

open class RayPointLight(
    level: Level,
    @JvmField
    val color: IVec3<Float>,
    @JvmField
    val flicker: Float,
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

    //? if 1.21 {
    override val absolutePos: IVec3<Float>
        get() = SableCompanion.INSTANCE.getContainingClient((pos.toDouble() + offset.toDouble()).toJOML())?.let { NeoVec3d(it.renderPose(Vibrancy.tickDelta).transformPosition((pos.toDouble() + offset.toDouble()).toJOML())).toFloat() } ?: (pos.toFloat() + offset)
    val absoluteBlockPos: IVec3<Float>
        get() = SableCompanion.INSTANCE.getContainingClient(pos.toDouble().toJOML())?.let { NeoVec3d(
            it.renderPose(
                Vibrancy.tickDelta
            ).transformPosition(pos.toDouble().toJOML())
        ).toFloat() } ?: pos.toFloat()
    //? } else {
    /*override val absolutePos: IVec3<Float>
        get() = pos.toFloat() + offset
    val absoluteBlockPos: IVec3<Float>
        get() = pos.toFloat()
    *///? }
    override val boundingBox: AbstractRect3<Int> = NeoRect3i(pos - radius.toInt(), pos + radius.toInt())
    @JvmField
    val sections: List<SectionPos> = SectionPos.betweenClosedStream(
        SectionPos.blockToSectionCoord(boundingBox.min.x),
        SectionPos.blockToSectionCoord(boundingBox.min.y).coerceAtLeast(level.minSection).coerceAtMost(level.maxSection),
        SectionPos.blockToSectionCoord(boundingBox.min.z),
        SectionPos.blockToSectionCoord(boundingBox.max.x),
        SectionPos.blockToSectionCoord(boundingBox.max.y).coerceAtLeast(level.minSection).coerceAtMost(level.maxSection),
        SectionPos.blockToSectionCoord(boundingBox.max.z)
    ).toList()

    fun blit(mesh: StaticOneStepBlockLightMeshManager, target: LightTexture, shadowBuffer: GlBuffer, gridBuffer: VoxelGridBuffer?, uniforms: GlBoundProgram.() -> Unit, shader: NeoIdentifier) {
        target.framebuffer.bind(NeoRect2i(0, 0, target.width!!, target.height!!)).use { fbo ->
            drawState(shader) {
                uniforms(this)

                setUniform("LightPos") { setFloatVec(offset) }
                setUniform("LightColor") { setFloatVec(color) }
                setUniform("LightRadius") { set(radius) }
                setUniform("ShadowRadius") { set(radius.coerceAtMost(VibrancyConfig.rayLightShadowRadius.toFloat()).toInt()) }

                setUniform("TextureSize") { set(target.width!!.toFloat(), target.height!!.toFloat()) }

                setShaderStorageBuffer("ShadowQuadBuffer", shadowBuffer)

                if (gridBuffer != null) {
                    setShaderStorageBuffer("GridBuffer", gridBuffer)
                }
            }.bind().use { mesh.lightMesh.draw() }
        }
    }

    var meshData: LightMesh.ComplexMeshData? = null
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
    val mesh = StaticOneStepBlockLightMeshManager(
        pos,
        {
            NeoRect3i(
                NeoVec3i(-radius.toInt(), -radius.toInt(), -radius.toInt()),
                NeoVec3i(radius.toInt(), radius.toInt(), radius.toInt())
            )
        },
        {
            val r = ceil(radius.coerceAtMost(VibrancyConfig.rayLightShadowRadius.toFloat())).toInt()
            NeoRect3i(NeoVec3i(-r, -r, -r), NeoVec3i(r, r, r))
        }
    ) { mesh, info, profiler ->
        meshData = info

        profiler.push("resize")
        staticTexture.resize(info.sections.size.x, info.sections.size.y)
        dynamicTexture.resize(info.sections.size.x, info.sections.size.y)
        profiler.pop()

        profiler.push("bindFramebuffer")
        val bound = staticTexture.framebuffer.bind(NeoRect2i(0, 0, staticTexture.width!!, staticTexture.height!!))
        profiler.pop()

        profiler.push("clearStatic")
        staticTexture.clear()
        profiler.pop()

        profiler.push("blit")
        blit(
            mesh,
            staticTexture,
            mesh.lightMesh.mesh.vbo,
            mesh.gridBuffer,
            {
                val atlas = NeoAtlas.blocks
                setTexture(
                    0,
                    GlTextureBinding.FromInstance(
                        atlas,
                        GlTextureTarget.TEXTURE_2D
                    )
                )
                setUniform("Sampler0Size") { set(atlas.width, atlas.height) }
            },
            Vibrancy.id("block/raytraced/blit")
        )
        profiler.pop()

        profiler.push("unbindFramebuffer")
        bound.free()
        profiler.pop()

        profiler.push("clearDynamic")
        dynamicTexture.clear()
        profiler.pop()
    }

    constructor(level: Level, info: RayPointLightInfo, state: BlockState, pos: IVec3<Int>) : this(
        level,
        info.color(state) * info.brightness(state),
        info.flicker(state),
        info.radius(state),
        info.offset(state),
        pos
    )

    fun reload() {
        mesh.queueMesh()
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
        val box = boundingBox

        for (section in manager.dirtySections) {
            if (section.second.min.allLessThan(box.max) && section.second.max.allGreaterThan(box.min)) { // TODO
                mesh.queueMesh()
                break
            }
        }

        profiler.pop()

        profiler.push("tickAsync")
        mesh.tick(
            data,
            manager,
            profiler
        )
        profiler.pop()

        if (dynamicShadows) {
            profiler.push("dynamicShadows")
            manager.getLevel()?.let { level ->
                profiler.push("collect")

                dynamicCleared = false

                val absolutePos = absolutePos
                val absoluteBlockPos = absoluteBlockPos

                val allTextures = hashSetOf<NeoIdentifier>()

                data class Node(
                    val buffers: MutableMap<NeoIdentifier, Pair<DynamicLightFace.Consumer, MutableList<DynamicLightFace>>> = hashMapOf(),
                    val bufferSource: NeoMultiBufferSource = NeoMultiBufferSource { settings: NeoRenderSettings ->
                        val texture = settings.drawState.shader.textures.getOrNull(0)?.location ?: return@NeoMultiBufferSource EmptyVertexConsumer

                        if (Vibrancy.entityShadowTextureBlacklist.contains(texture)) {
                            return@NeoMultiBufferSource EmptyVertexConsumer
                        }

                        if (GlTexture2D[texture] == null) {
                            return@NeoMultiBufferSource EmptyVertexConsumer
                        }

                        allTextures.add(texture)

                        buffers.computeIfAbsent(texture) {
                            val list = arrayListOf<DynamicLightFace>()
                            DynamicLightFace.Consumer(list::add) to list
                        }.first
                    }
                ) {
                    fun getQuads(textures: List<NeoIdentifier>) = textures.mapIndexedNotNull { index, texture -> buffers[texture]?.second?.map { it to index } }.flatten()

                    fun computeBox(textures: List<NeoIdentifier>): AbstractRect3<Float>? {
                        var minX: Float? = null
                        var minY: Float? = null
                        var minZ: Float? = null
                        var maxX: Float? = null
                        var maxY: Float? = null
                        var maxZ: Float? = null

                        for (texture in textures) {
                            buffers[texture]?.let { builder ->
                                for (face in builder.second) {
                                    face.apply { vertex ->
                                        minX = minX?.let { vertex.x.coerceAtMost(it) } ?: vertex.x
                                        minY = minY?.let { vertex.y.coerceAtMost(it) } ?: vertex.y
                                        minZ = minZ?.let { vertex.z.coerceAtMost(it) } ?: vertex.z

                                        maxX = maxX?.let { vertex.x.coerceAtLeast(it) } ?: vertex.x
                                        maxY = maxY?.let { vertex.y.coerceAtLeast(it) } ?: vertex.y
                                        maxZ = maxZ?.let { vertex.z.coerceAtLeast(it) } ?: vertex.z
                                    }
                                }
                            }
                        }

                        if (minX != null && minY != null && minZ != null && maxX != null && maxY != null && maxZ != null) {
                            return NeoRect3f(minX, minY, minZ, maxX, maxY, maxZ)
                        }

                        return null
                    }
                }

                val nodes = arrayListOf<Node>()
                val poseStack = PoseStack()

                poseStack.pushPose()

                //? if 1.21 {
                val subLevel = SableCompanion.INSTANCE.getContainingClient(pos.toDouble().toJOML())
                val subLevelPose = subLevel?.renderPose()

                if (subLevelPose != null) {
                    poseStack.mulPose(Quaternionf(subLevelPose.orientation()).invert())
                }
                //? }

                poseStack.translate(-absoluteBlockPos.x, -absoluteBlockPos.y, -absoluteBlockPos.z)

                Vibrancy.disableFlywheelInstancing = true
                if (VibrancyConfig.entityShadowsEnabled) {
                    profiler.push("entityShadows")
                    for (entity in level.getEntities(null, AABB.ofSize(Vec3(absolutePos.toJOML()), radius.toDouble() * 2, radius.toDouble() * 2, radius.toDouble() * 2))) {
                        if (boundingBox.contains(NeoVec3i(entity.blockPosition()))) {
                            val node = Node()
                            debugOut("entityShadows", 1)
                            EntityRenderingUtil.render(entity, poseStack, node.bufferSource)
                            nodes.add(node)
                        }
                    }
                    profiler.pop()
                }

                if (VibrancyConfig.blockEntityShadows) {
                    profiler.push("blockEntityShadows")

                    for (pos in mesh.getBlockEntities(level)) {
                        level.getBlockEntity(pos)?.let { blockEntity ->
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

                            EntityRenderingUtil.renderBlockEntity(blockEntity, poseStack, node.bufferSource, data)

                            poseStack.popPose()
                            nodes.add(node)
                        }
                    }

                    profiler.pop()
                }
                Vibrancy.disableFlywheelInstancing = false

                poseStack.popPose()
                profiler.pop()

                profiler.push("calculate")
                dynamicTexture.framebuffer.bind(NeoRect2i(0, 0, dynamicTexture.width!!, dynamicTexture.height!!)).use { fbo ->
                    var cleared = false

                    nodes.forEach { it.buffers.forEach { (texture, consumer) -> consumer.first.flush() } }

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

                            debugOut("numDynamicShadowFaces", quads.size)

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
                            blit(
                                mesh,
                                dynamicTexture,
                                dynamicBuffer,
                                null,
                                {
                                    setTextureArray(0, "Samplers", *textures.map { GlTextureBinding.FromInstance(it, GlTextureTarget.TEXTURE_2D) }.toTypedArray())
                                    setShaderStorageBuffer("BVHBuffer", dynamicBVHBuffer)
                                    setShaderStorageBuffer("TextureInfoBuffer", dynamicTextureInfoBuffer)
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

    fun render(data: RenderEventData, shader: GlBoundProgram, atlas: NeoAtlas, debugOut: (key: String, value: Int) -> Unit, profiler: ProfilerFiller) {
        debugOut("lightsRendered", 1)
        debugOut("numAsyncTasks", mesh.numActiveTasks())

        profiler.push("transforms")
        //? if 1.21 {
        val subLevel = SableCompanion.INSTANCE.getContainingClient(pos.toDouble().toJOML())

        if (subLevel == null) {
            shader.setUniform("ModelViewMat") { set(data.modelViewMat) }
            shader.setUniform("SpecularMat") { set(Matrix4f()) }
            shader.setUniform("CameraPos") { setFloatVec(data.camera.pos - pos.toFloat()) }
        } else {
            val pose = subLevel.renderPose(Vibrancy.tickDelta)
            val orientation = Quaternionf(pose.orientation())
            val pos = NeoVec3d(pose.transformPosition(pos.toDouble().toJOML()))
            shader.setUniform("ModelViewMat") {
                set(
                    data.modelViewMat.rotate(orientation, Matrix4f())
                )
            }
            shader.setUniform("SpecularMat") {
                set(
                    Matrix4f().rotate(orientation)
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
        shader.setUniform("LightFlicker") { set(flicker * VibrancyConfig.flickerStrength) }
        shader.setUniform("GLFWTime") { set(glfwGetTime().toFloat()) }

        shader.setUniform("LightPos") { setFloatVec(offset) }
        shader.setUniform("LightColor") { setFloatVec(color) }
        shader.setUniform("LightRadius") { set(radius) }
        shader.setTexture(0, GlTextureBinding.FromInstance(
            atlas,
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

        debugOut("numLightFaces", mesh.lightMesh.mesh.size / 6)
    }
}