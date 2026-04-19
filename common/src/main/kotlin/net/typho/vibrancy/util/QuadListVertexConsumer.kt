package net.typho.vibrancy.util

import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad

open class QuadListVertexConsumer(
    @JvmField
    val list: MutableList<NeoBakedQuad> = arrayListOf(),
    @JvmField
    val isEnabled: () -> Boolean = { true }
) : NeoBakedQuad.Consumer() {
    override fun take(quad: NeoBakedQuad) {
        if (isEnabled()) {
            list.add(quad)
        }
    }
}