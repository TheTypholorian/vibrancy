package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Vector2f
import org.joml.Vector3f
import java.util.*

class ShadowBuilder : VertexConsumer {
    val vertices = LinkedList<VertexInfo>()
    private var vertex: Vector3f? = null
    private var uv: Vector2f? = null

    fun endVertex() {
        vertex?.let {
            vertices.add(VertexInfo(it, uv!!))
            vertex = null
            uv = null
        }
    }

    override fun addVertex(
        p0: Float,
        p1: Float,
        p2: Float
    ): VertexConsumer {
        endVertex()
        vertex = Vector3f(p0, p1, p2)
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
        uv = Vector2f(p0, p1)
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

    class VertexInfo(
        var vertex: Vector3f,
        var uv: Vector2f
    )
}