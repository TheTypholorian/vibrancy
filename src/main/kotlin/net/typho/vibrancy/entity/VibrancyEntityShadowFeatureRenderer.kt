package net.typho.vibrancy.entity

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureImpl
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.feature.FeatureFrameContext
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.feature.FeatureRenderer
import net.minecraft.client.renderer.feature.FeatureRendererType
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer
import net.minecraft.client.renderer.feature.submit.SubmitNode
import net.minecraft.client.renderer.rendertype.PreparedRenderType
import net.minecraft.client.renderer.state.GameRenderState
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.client.resources.model.sprite.AtlasManager
import net.minecraft.resources.Identifier
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.GpuTexture
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuTextureUsage
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.mixin.FeatureRenderDispatcherAccessor
import net.typho.vibrancy.mixin.LevelRendererAccessor
import net.typho.vibrancy.util.ExtraAtlases
import org.joml.Vector4f
import java.util.Optional
import java.util.OptionalDouble
import java.util.function.Supplier

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
    protected val storage = Storage()
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
        submits: List<Submit<*>>,
        strictlyOrdered: Boolean
    ) {
        for (submit in submits) {
            submit.renderer.submit(submit.state, submit.pose, storage, (Minecraft.getInstance().levelRenderer as LevelRendererAccessor).`vibrancy$getLevelRenderState`().cameraRenderState)
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

            RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                { "Vibrancy Entity Shadows" },
                texture,
                Optional.of(Vector4f(0f)),
                target.depthTextureView,
                OptionalDouble.empty()
            ).use { pass ->
                pass.setPipeline(Vibrancy.entityShadowRenderType.pipeline())

                pass.setUniform("Globals", RenderSystem.getGlobalSettingsUniform()!!)
                pass.setUniform("u_VibrancyConfig", configBuffer)

                for (draw in draws) {
                    pass.bindTexture("u_BaseTex", draw.texture.textureView, draw.texture.sampler)
                    pass.bindTexture("u_TransmissionTex", draw.transmissionTex ?: draw.texture.textureView, draw.texture.sampler)

                    pass.setVertexBuffer(0, draw.info.vertexBuffer.slice()) // TODO use block models
                    pass.setIndexBuffer(draw.info.indexBuffer, draw.info.indexType)

                    pass.drawIndexed(draw.info.indexCount, 1, draw.info.firstIndex, draw.info.baseVertex, 0)
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

    open class Storage : SubmitNodeStorage()

    open class Submit<S : EntityRenderState>(
        @JvmField
        val renderer: EntityRenderer<*, in S>,
        @JvmField
        val state: S,
        @JvmField
        val pose: PoseStack,
        //@JvmField
        //val boundingBox: BlockBox
    ) : SubmitNode {
        override fun featureType(): FeatureRendererType<out Submit<*>> {
            return TYPE
        }
    }
}