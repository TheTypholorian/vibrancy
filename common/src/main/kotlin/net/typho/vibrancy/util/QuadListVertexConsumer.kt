package net.typho.vibrancy.util

import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad

open class QuadListVertexConsumer(
    @JvmField
    val list: MutableList<NeoBakedQuad> = arrayListOf()
) : NeoBakedQuad.Consumer() {
    override fun take(quad: NeoBakedQuad) {
        list.add(quad)
    }
}