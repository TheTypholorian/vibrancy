package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.util.MultiBufferSourceInjection
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoRenderSettings
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.math.vec.IVec2
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightInfo

abstract class RaytracedGuiGraphics(
    @JvmField
    protected val minecraft: Minecraft,
    @JvmField
    protected val buffers: MultiBufferSource.BufferSource
) : GuiGraphics(minecraft, buffers), MultiBufferSourceInjection {
    companion object {
        @JvmField
        val shadowBuffer = ShadowBuffer(GlBufferUsage.STREAM_DRAW)
    }

    @JvmField
    val builders = hashMapOf<NeoRenderSettings, NeoBakedQuad.Consumer?>()
    @JvmField
    val blitBuilders = hashMapOf<GlTexture2D, NeoBakedQuad.Consumer?>()
    @JvmField
    val lights = arrayListOf<Light>()
    @JvmField
    var collect = true

    init {
        WrapperUtil.INSTANCE.inject(buffers, this)
    }

    abstract fun createConsumer(settings: NeoRenderSettings): NeoBakedQuad.Consumer?

    abstract fun createConsumer(texture: GlTexture2D): NeoBakedQuad.Consumer?

    fun renderRaytraced(quads: Map<NeoRenderSettings, List<NeoBakedQuad>>, blitQuads: Map<GlTexture2D, List<NeoBakedQuad>>) {
        if (VibrancyConfig.inventoryLightsShadows) {
            val shadowQuads = quads.filter { (settings, quads) -> settings.drawState.shader.textures.getOrNull(0)?.texture == NeoAtlas.blocks }
                .flatMap { it.value }

            shadowBuffer.lazyUploadQuads(NeoAtlas.blocks.width, NeoAtlas.blocks.height, shadowQuads)()
        } else {
            shadowBuffer.bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(0L, GlBufferUsage.STREAM_DRAW) }
        }

        for (light in lights) {
            light.light.renderInventoryLight(
                light.absolutePos,
                light.shadowPos,
                minecraft.window.width,
                minecraft.window.height,
                light.item,
                light.block,
                quads,
                blitQuads,
                WrapperUtil.INSTANCE.wrap(buffers)
            )
        }
    }

    override fun getBuffer(settings: NeoRenderSettings): NeoVertexConsumer? {
        return if (settings.mode == GlBeginMode.QUADS) {
            builders.computeIfAbsent(settings, ::createConsumer)
        } else {
            null
        }
    }

    override fun endBatch(settings: NeoRenderSettings) {
        builders[settings]?.flush()
    }

    fun flushRaytraced() {
        builders.forEach { (settings, consumer) -> consumer?.flush() }
    }

    fun end() {
        WrapperUtil.INSTANCE.uninject(buffers, this)
    }

    data class Light(
        @JvmField
        val absolutePos: IVec2<Int>,
        @JvmField
        val shadowPos: IVec3<Float>,
        @JvmField
        val item: ItemStack,
        @JvmField
        val block: BlockState,
        @JvmField
        val light: BlockLightInfo
    )
}