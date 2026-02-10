package net.typho.vibrancy.util

import com.mojang.blaze3d.vertex.VertexConsumer

object EmptyVertexConsumer : VertexConsumer {
    override fun addVertex(
        p0: Float,
        p1: Float,
        p2: Float
    ): VertexConsumer {
        return this
    }

    override fun setColor(
        p0: Int,
        p1: Int,
        p2: Int,
        p3: Int
    ): VertexConsumer {
        return this
    }

    override fun setUv(p0: Float, p1: Float): VertexConsumer {
        return this
    }

    override fun setUv1(p0: Int, p1: Int): VertexConsumer {
        return this
    }

    override fun setUv2(p0: Int, p1: Int): VertexConsumer {
        return this
    }

    override fun setNormal(
        p0: Float,
        p1: Float,
        p2: Float
    ): VertexConsumer {
        return this
    }
}