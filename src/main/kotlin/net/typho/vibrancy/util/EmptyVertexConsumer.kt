package net.typho.vibrancy.util

import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexConsumer

object EmptyVertexConsumer : NeoVertexConsumer() {
    override fun color(
        r: Int,
        g: Int,
        b: Int,
        a: Int
    ): NeoVertexConsumer {
        return this
    }

    override fun lightUV(
        u: Int,
        v: Int
    ): NeoVertexConsumer {
        return this
    }

    override fun normal(
        x: Float,
        y: Float,
        z: Float
    ): NeoVertexConsumer {
        return this
    }

    override fun overlayUV(
        u: Int,
        v: Int
    ): NeoVertexConsumer {
        return this
    }

    override fun textureUV(
        u: Float,
        v: Float
    ): NeoVertexConsumer {
        return this
    }

    override fun vertex(
        x: Float,
        y: Float,
        z: Float
    ): NeoVertexConsumer {
        return this
    }
}