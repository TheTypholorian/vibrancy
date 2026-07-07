package net.typho.vibrancy.entity

import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBufferImpl
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureImpl
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.feature.*
import net.minecraft.client.renderer.feature.submit.SubmitNode
import net.minecraft.client.renderer.rendertype.PreparedRenderType
import net.minecraft.client.renderer.state.GameRenderState
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.client.resources.model.sprite.AtlasManager
import net.minecraft.core.BlockBox
import net.minecraft.core.SectionPos
import net.minecraft.data.AtlasIds
import net.minecraft.resources.Identifier
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.GpuTexture
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
import org.joml.Vector4f
import java.util.*
import java.util.function.Supplier
import kotlin.collections.isNotEmpty

open class VibrancyEntityShadowFeatureRenderer : FeatureRenderer<VibrancyEntityShadowFeatureRenderer.Submit> {
    companion object {
        @JvmField
        val TYPE = FeatureRendererType.create<Submit>("Vibrancy Entity Shadows")
    }

    @JvmField
    protected var texture: GpuTexture? = null
    @JvmField
    protected var textureView: GpuTextureView? = null
    @JvmField
    protected var vertexBuffer: GpuBuffer? = null
    protected val dispatcher by lazy {
        Dispatcher(
            Minecraft.getInstance().modelManager,
            Minecraft.getInstance().atlasManager,
            Minecraft.getInstance().font,
            Minecraft.getInstance().gameRenderer.gameRenderState()
        )
    }

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
        submits: List<Submit>,
        strictlyOrdered: Boolean
    ) {
    }

    override fun executeGroup(
        context: FeatureFrameContext,
        groupIndex: Int,
        submits: List<Submit>,
        strictlyOrdered: Boolean
    ) {
        val target = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val texture = getTexture(target.width, target.height)
        val sections = mutableMapOf<SectionPos, SectionMeshCache?>()
        val configBuffer = VibrancyConfig.loadConfigBuffer()
        val transmissionTextures = mutableMapOf<Identifier, GpuTextureView?>()
        val camera = (Minecraft.getInstance().levelRenderer as LevelRendererAccessor).`vibrancy$getLevelRenderState`().cameraRenderState

        data class BlockMesh(
            @JvmField
            val vertexBuffer: GpuBufferSlice,
            @JvmField
            val indexBuffer: GpuBufferSlice,
            @JvmField
            val indexType: IndexType
        )

        data class Draw(
            @JvmField
            val blockMesh: BlockMesh,
            @JvmField
            val info: StagedVertexBuffer.ExecuteInfo,
            @JvmField
            val renderType: PreparedRenderType,
            @JvmField
            val texture: PreparedRenderType.Texture,
            @JvmField
            val transmissionTex: GpuTextureView?
        )

        val blockMeshes = mutableListOf<BlockMesh>()
        val draws = mutableListOf<Draw>()

        ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE).use { byteBufferBuilder ->
            val builder = BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR) // TODO replace with persistent mapping
            var totalVertices = 0L
            var totalIndices = 0L
            val blockMeshRanges = submits.map { submit ->
                val ret = totalVertices to totalIndices

                for (pos in submit.boundingBox) {
                    sections.computeIfAbsent(SectionPos.of(pos)) { key ->
                        synchronized(Vibrancy.lightManager) {
                            Vibrancy.lightManager.sectionMeshCaches[key]
                        }
                    }?.let { section ->
                        section[pos]?.solidFaces?.forEach { face ->
                            if (PackedNormal.unpackByteY(face.v0.normal) >= 0) {
                                totalVertices += 4
                                totalIndices += 6
                                face.apply { vertex ->
                                    builder.addVertex(
                                        (vertex.x + pos.x - camera.pos.x).toFloat(),
                                        (vertex.y + pos.y - camera.pos.y).toFloat(),
                                        (vertex.z + pos.z - camera.pos.z).toFloat()
                                    )
                                        .setUv(vertex.u, vertex.v)
                                        .setLight(vertex.light)
                                        .setColor(vertex.color)
                                }
                            }
                        }
                    }
                }

                ret to ((totalVertices - ret.first) to (totalIndices - ret.second))
            }

            val built = builder.build() ?: return

            val vertexBuffer1 = vertexBuffer
            val vertexBuffer = if (vertexBuffer1 == null || vertexBuffer1.size < built.vertexBufferSlice().size()) {
                vertexBuffer1?.recycle()
                val buffer = GpuObjects.buffer({ "Vibrancy Entity Shadows Vertex Buffer" }, GpuBufferUsage.VERTEX or GpuBufferUsage.COPY_DST, MemoryPointer.wrap(built.vertexBufferSlice().byteBuffer()))
                vertexBuffer = buffer
                buffer
            } else {
                vertexBuffer1.upload(built.vertexBufferSlice().byteBuffer())
                vertexBuffer1
            }

            val indexBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).let { it.getBuffer(built.drawState().indexCount) to it.type() }

            for (offset in blockMeshRanges) {
                blockMeshes.add(BlockMesh(
                    vertexBuffer.slice(offset.first.first, offset.second.first),
                    indexBuffer.first.slice(offset.first.second, offset.second.second),
                    indexBuffer.second
                ))
            }
        }

        for ((submit, blockMesh) in submits.zip(blockMeshes)) {
            dispatcher.prepareFrame(submit).use { frame ->
                (dispatcher as FeatureRenderDispatcherAccessor).`vibrancy$getFeatureRenderers`()
                    .values()
                    .filterIsInstance<RenderTypeFeatureRenderer<*>>()
                    .flatMapTo(draws) { renderer ->
                        renderer.groups.flatMap { group ->
                            group.draws.zip(group.drawRenderTypes).mapNotNull { (draw, renderType) ->
                                @Suppress("KotlinConstantConditions")
                                dispatcher.vertexBuffer.getExecuteInfo(draw)?.let { info ->
                                    (renderType.textures.find { it.name == "Sampler0" } ?: renderType.textures.firstOrNull())?.let { texture ->
                                        (texture as PreparedRenderTypeTextureExtension).`vibrancy$identifier`?.let { textureId ->
                                            Draw(
                                                blockMesh,
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
            }
        }

        if (draws.isNotEmpty()) {
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

                    pass.setVertexBuffer(0, (draw.blockMesh.vertexBuffer as GpuBufferImpl).slice())
                    pass.setIndexBuffer(draw.blockMesh.indexBuffer.buffer, draw.blockMesh.indexType)

                    pass.drawIndexed(draw.blockMesh.indexBuffer.length.toInt(), 1, 0, 0, 0)
                }
            }

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

    class SubmitRef @JvmOverloads constructor(
        @JvmField
        var submit: Submit? = null
    )

    open class Submit(
        @JvmField
        var boundingBox: BlockBox
    ) : SubmitNodeStorage(), SubmitNode {
        override fun featureType(): FeatureRendererType<out Submit> {
            return TYPE
        }
    }
}