package net.typho.vibrancy.shadows

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemStack
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.util.MultiBufferSourceInjection
import net.typho.big_shot_lib.api.client.rendering.util.NeoRenderSettings
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.vibrancy.block.BlockLightInfo
import org.lwjgl.system.NativeResource

abstract class RaytracedGuiGraphics(
    minecraft: Minecraft,
    @JvmField
    protected val buffers: MultiBufferSource.BufferSource
) : GuiGraphics(minecraft, buffers), MultiBufferSourceInjection, NativeResource {
    @JvmField
    val builders = hashMapOf<NeoRenderSettings, NeoBakedQuad.Consumer?>()
    @JvmField
    val lights = arrayListOf<Light>()

    init {
        WrapperUtil.INSTANCE.inject(buffers, this)
    }

    abstract fun createConsumer(settings: NeoRenderSettings): NeoBakedQuad.Consumer?

    fun renderRaytraced() {
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

    override fun free() {
        WrapperUtil.INSTANCE.uninject(buffers, this)
    }

    data class Light(
        @JvmField
        val x: Int,
        @JvmField
        val y: Int,
        @JvmField
        val item: ItemStack,
        @JvmField
        val light: BlockLightInfo
    )
}