package net.typho.vibrancy.block

import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.util.NeoMultiBufferSource
import net.typho.big_shot_lib.api.client.rendering.util.NeoRenderSettings
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.math.vec.IVec2
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.util.StateFunction

interface BlockLightInfo {
    val type: BlockLightType<*, *>
    val enabled: StateFunction<Boolean>

    fun renderInventoryLight(
        absolutePos: IVec2<Int>,
        shadowPos: IVec3<Float>,
        width: Int,
        height: Int,
        stack: ItemStack,
        block: BlockState,
        quads: Map<NeoRenderSettings, List<NeoBakedQuad>>,
        blitQuads: Map<GlTexture2D, List<NeoBakedQuad>>,
        buffers: NeoMultiBufferSource
    ): Boolean = false
}