package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.util.Mth
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
import net.typho.big_shot_lib.api.client.rendering.util.BlockChunkLayer
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoRenderSettings
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.math.NeoDirection
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.rect.NeoRect3f
import net.typho.big_shot_lib.api.math.rect.NeoRect3i
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
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
        fun drawState(texture: GlTextureBinding, shader: NeoIdentifier) = GlDrawState.Basic(
            blend = GlBlendShard.Enabled(
                BlendFunction.Basic(
                    GlBlendingFactor.DST_COLOR,
                    GlBlendingFactor.ZERO
                ),
                GlBlendEquation.ADD
            ),
            shader = GlShaderShard.FromLocation(
                shader,
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

    fun blit(target: LightTexture, shadowBuffer: ShadowBuffer, materialTexture: GlTextureBinding, shader: NeoIdentifier) {
        target.framebuffer.bind(NeoRect2i(0, 0, target.width, target.height)).use { fbo ->
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, shadowBuffer.glId)

            drawState(materialTexture, shader).bind().use { drawState ->
                drawState.shader.setUniform("LightPos") { set(absolutePos) }
                drawState.shader.setUniform("LightColor") { set(color * VibrancyConfig.rayLightBrightness) }
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
    val dynamicBVHBuffer = NeoGlBuffer()
    protected var dynamicCleared = true

    val staticTexture = LightTexture()
    val mesh = StaticBlockLightMeshManager { mesh, info ->
        meshData = info
        staticTexture.resize(info.sections.size.x, info.sections.size.y)
        dynamicTexture.resize(info.sections.size.x, info.sections.size.y)
        LightMesh.initBlitMesh(blitMesh, info)
        staticTexture.framebuffer.bind(NeoRect2i(0, 0, staticTexture.width, staticTexture.height)).use { fbo ->
            staticTexture.clear()
            blit(
                staticTexture,
                mesh.shadowBuffer,
                GlTextureBinding.FromInstance(
                    NeoAtlas.blocks,
                    GlTextureTarget.TEXTURE_2D
                ),
                Vibrancy.id("block/raytraced/blit")
            )
        }
        dynamicTexture.clear()
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
            val shadowRadius = ceil(radius.coerceAtMost(VibrancyConfig.rayLightShadowRadius.toFloat())).toInt()
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

            val state = state ?: level.getBlockState(pos.blockPos)

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

            val state = state ?: level.getBlockState(pos.blockPos)

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
        staticTexture.free()
        mesh.free()
    }

    fun update(manager: LightManager, debugOut: (String, Int) -> Unit, dynamicShadows: Boolean) {
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
            manager.getLevel()?.let { level ->
                dynamicCleared = false

                val allTextures = hashSetOf<NeoIdentifier>()

                data class Node(
                    val quads: MutableMap<NeoIdentifier, MutableList<NeoBakedQuad>> = hashMapOf(),
                    val buffers: MutableMap<NeoIdentifier, NeoBakedQuad.Consumer> = hashMapOf(),
                    val bufferSource: MultiBufferSource = WrapperUtil.INSTANCE.unwrap { settings: NeoRenderSettings ->
                        val texture = settings.drawState.shader.textures[0].location!!
                        allTextures.add(texture)

                        buffers.computeIfAbsent(texture) {
                            val quads = quads.computeIfAbsent(texture) { texture -> arrayListOf() }
                            object : NeoBakedQuad.Consumer() {
                                override fun take(quad: NeoBakedQuad) {
                                    quads.add(quad)
                                }
                            }
                        }
                    }
                ) {
                    fun computeBox(texture: NeoIdentifier): AbstractRect3<Float> {
                        quads[texture]?.let { builder ->
                            val min = builder.fold(null) { accum: IVec3<Float>?, quad -> quad.v0.pos.min(quad.v1.pos.min(quad.v2.pos.min(if (accum == null) quad.v3.pos else quad.v3.pos.min(accum)))) }
                            val max = builder.fold(null) { accum: IVec3<Float>?, quad -> quad.v0.pos.max(quad.v1.pos.max(quad.v2.pos.max(if (accum == null) quad.v3.pos else quad.v3.pos.max(accum)))) }

                            if (min != null && max != null) {
                                return NeoRect3f(min, max)
                            }
                        }

                        throw NullPointerException()
                    }
                }

                val nodes = arrayListOf<Node>()
                val tickDelta = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(false)
                val poseStack = PoseStack()

                for (entity in level.getEntities(null, AABB.ofSize(Vec3(absolutePos.toJOML()), radius.toDouble() * 2, radius.toDouble() * 2, radius.toDouble() * 2))) {
                    if (meshCollector.checked.contains(NeoVec3i(entity.blockPosition()))) {
                        val node = Node()
                        debugOut("entityShadows", 1)
                        Minecraft.getInstance().entityRenderDispatcher.render(
                            entity,
                            Mth.lerp(tickDelta.toDouble(), entity.xOld, entity.x),
                            Mth.lerp(tickDelta.toDouble(), entity.yOld, entity.y),
                            Mth.lerp(tickDelta.toDouble(), entity.zOld, entity.z),
                            Mth.lerp(tickDelta, entity.yRotO, entity.yRot),
                            tickDelta,
                            poseStack,
                            node.bufferSource,
                            net.minecraft.client.renderer.LightTexture.FULL_BRIGHT
                        )
                        nodes.add(node)
                    }
                }

                if (VibrancyConfig.blockEntityShadows) {
                    for (pos in meshCollector.blockEntities) {
                        level.getBlockEntity(pos.blockPos)?.let { blockEntity ->
                            val node = Node()
                            debugOut("blockEntityShadows", 1)

                            poseStack.pushPose()
                            poseStack.translate(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())

                            Minecraft.getInstance().blockEntityRenderDispatcher.render(
                                blockEntity,
                                tickDelta,
                                poseStack,
                                node.bufferSource
                            )

                            poseStack.popPose()
                            nodes.add(node)
                        }
                    }
                }

                dynamicTexture.framebuffer.bind(NeoRect2i(0, 0, dynamicTexture.width, dynamicTexture.height)).use { fbo ->
                    var cleared = false

                    nodes.forEach { it.buffers.forEach { (texture, consumer) -> consumer.flush() } }

                    for (texture in allTextures) {
                        val nodes = nodes.filter { it.quads.containsKey(texture) }

                        if (nodes.isNotEmpty()) {
                            val bvhBuffer = NeoBuffer.Native(nodes.size * 32L)

                            bvhBuffer.write().run {
                                var index = 0

                                for (node in nodes) {
                                    val box = node.computeBox(texture)

                                    writeFloat(box.min.x)
                                    writeFloat(box.min.y)
                                    writeFloat(box.min.z)
                                    writeInt(index)

                                    writeFloat(box.max.x)
                                    writeFloat(box.max.y)
                                    writeFloat(box.max.z)
                                    index += node.quads[texture]!!.size
                                    writeInt(index)

                                    //println("${node.builders[texture]!!.size} $texture")
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

                            val quads = nodes.flatMap { it.quads[texture]!! }
                            val texture = GlTexture2D[texture]!!

                            dynamicBuffer.lazyUploadQuads(texture.width, texture.height, quads)()
                            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, dynamicBVHBuffer.glId)
                            blit(
                                dynamicTexture,
                                dynamicBuffer,
                                GlTextureBinding.FromInstance(
                                    texture,
                                    GlTextureTarget.TEXTURE_2D
                                ),
                                Vibrancy.id("block/raytraced/dynamic_blit")
                            )
                        }

                        /*
                        if (builder.value.isNotEmpty()) {
                            if (!cleared) {
                                dynamicTexture.clear()
                                cleared = true
                            }

                            val texture = GlTexture2D[builder.key]!!

                            dynamicBuffer.lazyUploadQuads(texture.width, texture.height, builder.value)()
                            blit(
                                dynamicTexture,
                                dynamicBuffer,
                                GlTextureBinding.FromInstance(
                                    texture,
                                    GlTextureTarget.TEXTURE_2D
                                ),
                                Vibrancy.id("block/raytraced/dynamic_blit")
                            )
                        }
                         */
                    }
                }

                debugOut("lightsWithEntityShadows", 1)
            }
        } else {
            if (!dynamicCleared) {
                dynamicTexture.clear()
                dynamicCleared = true
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