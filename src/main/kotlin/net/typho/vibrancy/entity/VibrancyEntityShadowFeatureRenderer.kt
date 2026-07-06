package net.typho.vibrancy.entity

import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBufferImpl
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureImpl
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexSorting
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatterImpls.brightness
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhase
import net.fabricmc.fabric.api.networking.v1.PlayerLookup.level
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.Lightmap
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.feature.*
import net.minecraft.client.renderer.feature.submit.SubmitNode
import net.minecraft.client.renderer.rendertype.PreparedRenderType
import net.minecraft.client.renderer.state.GameRenderState
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.client.resources.model.sprite.AtlasManager
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.data.AtlasIds
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.GpuTexture
import net.typho.big_shot_lib.api.client.rendering.common.Recyclable
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuTextureUsage
import net.typho.big_shot_lib.api.client.rendering.util.PackedNormal
import net.typho.big_shot_lib.api.util.buffer.MemoryPointer
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.mixin.FeatureRenderDispatcherAccessor
import net.typho.vibrancy.mixin.LevelRendererAccessor
import net.typho.vibrancy.util.ExtraAtlases
import net.typho.vibrancy.util.SectionMeshCache
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.function.Supplier
import kotlin.collections.mapTo

open class VibrancyEntityShadowFeatureRenderer : FeatureRenderer<VibrancyEntityShadowFeatureRenderer.Submit<*>> {
    companion object {
        @JvmField
        val TYPE = FeatureRendererType.create<Submit<*>>("Vibrancy Entity Shadows")
    }

    @JvmField
    protected var texture: GpuTexture? = null
    @JvmField
    protected var textureView: GpuTextureView? = null
    @JvmField
    protected var vertexBuffer: GpuBuffer? = null
    @JvmField
    protected var indexBuffer: Pair<GpuBuffer, IndexType>? = null
    @JvmField
    protected val storage = Storage()
    protected val dispatcher by lazy {
        Dispatcher(
            Minecraft.getInstance().modelManager,
            Minecraft.getInstance().atlasManager,
            Minecraft.getInstance().font,
            Minecraft.getInstance().gameRenderer.gameRenderState()
        )
    }

    data class BlockMesh(
        @JvmField
        val input: Set<BlockBox>,
        @JvmField
        val meshData: MeshData,
        @JvmField
        val vertexBuffer: GpuBuffer,
        @JvmField
        val indexBuffer: Pair<GpuBuffer, IndexType>
    )

    @JvmField
    protected var cachedBlockMesh: BlockMesh? = null

    open fun getTexture(width: Int, height: Int): GpuTextureView {
        texture?.let {
            if (it.width == width && it.height == height) {
                return textureView!!
            }

            textureView!!.close()
            it.recycle()
        }

        val texture = GpuObjects.texture({ "Vibrancy Entity Shadows" }, width, height, GpuTextureUsage.RENDER_ATTACHMENT or GpuTextureUsage.TEXTURE_BINDING)
        this.texture = texture
        val textureView = RenderSystem.getDevice().createTextureView(texture as GpuTextureImpl)
        this.textureView = textureView
        return textureView
    }

    override fun prepareGroup(
        context: FeatureFrameContext,
        submits: List<Submit<*>>,
        strictlyOrdered: Boolean
    ) {
        val stack = PoseStack()

        for (submit in submits) {
            stack.pushPose()
            stack.mulPose(submit.pose.pose())
            submit.renderer.submit(submit.state, stack, storage, (Minecraft.getInstance().levelRenderer as LevelRendererAccessor).`vibrancy$getLevelRenderState`().cameraRenderState)
            stack.popPose()
        }
    }

    override fun executeGroup(
        context: FeatureFrameContext,
        groupIndex: Int,
        submits: List<Submit<*>>,
        strictlyOrdered: Boolean
    ) {
        val target = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val texture = getTexture(target.width, target.height)

        data class Draw(
            @JvmField
            val info: StagedVertexBuffer.ExecuteInfo,
            @JvmField
            val renderType: PreparedRenderType,
            @JvmField
            val texture: PreparedRenderType.Texture,
            @JvmField
            val transmissionTex: GpuTextureView?
        )

        dispatcher.prepareFrame(storage).use { frame ->
            val configBuffer = VibrancyConfig.loadConfigBuffer()
            val transmissionTextures = mutableMapOf<Identifier, GpuTextureView?>()
            val draws = (dispatcher as FeatureRenderDispatcherAccessor).`vibrancy$getFeatureRenderers`()
                .values()
                .filterIsInstance<RenderTypeFeatureRenderer<*>>()
                .flatMap { renderer ->
                    renderer.groups.flatMap { group ->
                        group.draws.zip(group.drawRenderTypes).mapNotNull { (draw, renderType) ->
                            @Suppress("KotlinConstantConditions")
                            dispatcher.vertexBuffer.getExecuteInfo(draw)?.let { info ->
                                (renderType.textures.find { it.name == "Sampler0" } ?: renderType.textures.firstOrNull())?.let { texture ->
                                    (texture as PreparedRenderTypeTextureExtension).`vibrancy$identifier`?.let { textureId ->
                                        Draw(
                                            info,
                                            renderType,
                                            texture,
                                            transmissionTextures.computeIfAbsent(textureId) { key ->
                                                ExtraAtlases.getTransmission(key)?.textureView
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            val camera = (Minecraft.getInstance().levelRenderer as LevelRendererAccessor).`vibrancy$getLevelRenderState`().cameraRenderState

            val blockBoxes = submits.mapTo(mutableSetOf()) { it.boundingBox }

            val cachedBlockMesh = cachedBlockMesh
            val blockMesh = if (cachedBlockMesh == null || cachedBlockMesh.input != blockBoxes) {
                val builder = BufferBuilder(ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE), PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                val sections = mutableMapOf<SectionPos, SectionMeshCache?>()

                val collectedBlocks = mutableSetOf<BlockPos>()

                for (submit in submits) {
                    for (pos in submit.boundingBox) {
                        if (collectedBlocks.add(pos)) {
                            sections.computeIfAbsent(SectionPos.of(pos)) { key ->
                                synchronized(Vibrancy.lightManager) {
                                    Vibrancy.lightManager.sectionMeshCaches[key]
                                }
                            }?.let { section ->
                                section[pos]?.solidFaces?.forEach { face ->
                                    if (PackedNormal.unpackByteY(face.v0.normal) >= 0) {
                                        face.apply { vertex ->
                                            builder.addVertex(
                                                vertex.x + pos.x,
                                                vertex.y + pos.y,
                                                vertex.z + pos.z
                                            )
                                                .setUv(vertex.u, vertex.v)
                                                .setLight(vertex.light)
                                                .setColor(vertex.color)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val mesh = builder.build()?.use { mesh ->
                    val vertexBuffer1 = vertexBuffer
                    val vertexBuffer = if (vertexBuffer1 == null || vertexBuffer1.size < mesh.vertexBufferSlice().size()) {
                        vertexBuffer1?.recycle()
                        val buffer = GpuObjects.buffer({ "Vibrancy Entity Shadows Vertex Buffer" }, GpuBufferUsage.VERTEX or GpuBufferUsage.COPY_DST, MemoryPointer.wrap(mesh.vertexBufferSlice().byteBuffer()))
                        vertexBuffer = buffer
                        buffer
                    } else {
                        vertexBuffer1.upload(mesh.vertexBufferSlice().byteBuffer())
                        vertexBuffer1
                    }

                    val indexBuffer1 = indexBuffer
                    val indexBuffer = mesh.indexBuffer()?.let { meshIndexBuffer ->
                        if (indexBuffer1 == null || indexBuffer1.first.size < meshIndexBuffer.capacity()) {
                            indexBuffer1?.first?.recycle()
                            val buffer = GpuObjects.buffer({ "Vibrancy Entity Shadows Index Buffer" }, GpuBufferUsage.INDEX or GpuBufferUsage.COPY_DST, MemoryPointer.wrap(meshIndexBuffer)) to mesh.drawState().indexType
                            indexBuffer = buffer
                            buffer
                        } else {
                            indexBuffer1.first.upload(meshIndexBuffer)
                            indexBuffer1
                        }
                    } ?: RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).let { it.getBuffer(mesh.drawState().indexCount) to it.type() }

                    BlockMesh(blockBoxes, mesh, vertexBuffer, indexBuffer)
                }
                this.cachedBlockMesh = mesh
                mesh
            } else {
                cachedBlockMesh
            }

            if (blockMesh != null) {
                val stack = RenderSystem.getModelViewStack()
                stack.pushMatrix()
                stack.translate(-camera.pos.x.toFloat(), -camera.pos.y.toFloat(), -camera.pos.z.toFloat())
                RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    { "Vibrancy Entity Shadows" },
                    texture,
                    Optional.of(Vector4f(0f)),
                    target.depthTextureView,
                    OptionalDouble.empty()
                ).use { pass ->
                    val prepared = Vibrancy.entityShadowRenderType.prepare()
                    pass.setPipeline(prepared.pipeline)

                    RenderSystem.bindDefaultUniforms(pass)
                    pass.setUniform("DynamicTransforms", prepared.dynamicTransforms)
                    pass.setUniform("u_VibrancyConfig", configBuffer)
                    pass.bindTexture("u_BlockTex", Minecraft.getInstance().atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS).textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))

                    for (draw in draws) {
                        pass.bindTexture("u_TransmissionTex", draw.transmissionTex ?: draw.texture.textureView, draw.texture.sampler)

                        pass.setUniform("u_Shadows", draw.info.vertexBuffer.slice((draw.info.firstIndex / 6L * 4 + draw.info.baseVertex) * Vibrancy.entityShadowFormat.vertexSize, draw.info.indexCount / 6L * 4 * Vibrancy.entityShadowFormat.vertexSize))

                        pass.setVertexBuffer(0, (blockMesh.vertexBuffer as GpuBufferImpl).slice())
                        pass.setIndexBuffer(blockMesh.indexBuffer.first, blockMesh.indexBuffer.second)

                        pass.drawIndexed(blockMesh.meshData.drawState().indexCount, 1, 0, 0, 0)
                    }
                }
                stack.popMatrix()

                RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    { "Vibrancy Entity Shadow Blit" },
                    target.colorTextureView!!,
                    Optional.empty(),
                    target.depthTextureView,
                    OptionalDouble.empty()
                ).use { pass ->
                    val prepared = Vibrancy.entityShadowBlitRenderType.prepare()
                    pass.setPipeline(prepared.pipeline)

                    RenderSystem.bindDefaultUniforms(pass)
                    pass.bindTexture("u_ShadowTex", texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
                    pass.setUniform("u_VibrancyConfig", configBuffer)

                    pass.setVertexBuffer(0, (Vibrancy.blitVertexBuffer as GpuBufferImpl).slice())
                    pass.draw(6, 1, 0, 0)
                }
            }
        }
    }

    open class Dispatcher(
        modelManager: ModelManager,
        atlasManager: AtlasManager,
        font: Font,
        gameRenderState: GameRenderState
    ) : FeatureRenderDispatcher(
        Minecraft.getInstance().gameRenderer.renderBuffers(), // changed via mixin but we can't pass null
        modelManager,
        atlasManager,
        font,
        gameRenderState
    ) {
        @JvmField
        val vertexBuffer = (this as FeatureRenderDispatcherAccessor).`vibrancy$getStagedVertexBuffer`()
    }

    open class VertexBuffer(
        label: Supplier<String>,
        initialCapacity: Int
    ) : StagedVertexBuffer(label, initialCapacity)

    open class Storage : SubmitNodeStorage() {
        override fun <T : SubmitNode> submitCustom(phase: SubmitRenderPhase<T>, node: T) {
            // TODO
        }
    }

    open class Submit<S : EntityRenderState>(
        @JvmField
        val renderer: EntityRenderer<*, in S>,
        @JvmField
        val state: S,
        @JvmField
        val pose: PoseStack.Pose,
        @JvmField
        val boundingBox: BlockBox
    ) : SubmitNode {
        override fun featureType(): FeatureRendererType<out Submit<*>> {
            return TYPE
        }
    }
}