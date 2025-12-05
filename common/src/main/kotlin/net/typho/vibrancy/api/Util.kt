package net.typho.vibrancy.api

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.phys.AABB
import org.joml.Vector3f

fun boxOfRadius(center: Vector3f, radius: Float) = AABB(
    (center.x - radius).toDouble(),
    (center.y - radius).toDouble(),
    (center.z - radius).toDouble(),
    (center.x + radius).toDouble(),
    (center.y + radius).toDouble(),
    (center.z + radius).toDouble()
)

fun Vector3f.invert(): Vector3f {
    x = -x
    y = -y
    z = -z
    return this
}

fun VertexConsumer.cube(box: AABB) {
    val vertices = arrayOf(
        Vector3f(box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()),
        Vector3f(box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()),
        Vector3f(box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()),
        Vector3f(box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()),
        Vector3f(box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()),
        Vector3f(box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()),
        Vector3f(box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()),
        Vector3f(box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()),
    )

    addVertex(vertices[0]).setUv(0f, 1f).setNormal(0f, 0f, 1f)
    addVertex(vertices[1]).setUv(1f, 1f).setNormal(0f, 0f, 1f)
    addVertex(vertices[2]).setUv(1f, 0f).setNormal(0f, 0f, 1f)
    addVertex(vertices[3]).setUv(0f, 0f).setNormal(0f, 0f, 1f)

    addVertex(vertices[1]).setUv(0f, 1f).setNormal(-1f, 0f, 0f)
    addVertex(vertices[5]).setUv(1f, 1f).setNormal(-1f, 0f, 0f)
    addVertex(vertices[6]).setUv(1f, 0f).setNormal(-1f, 0f, 0f)
    addVertex(vertices[2]).setUv(0f, 0f).setNormal(-1f, 0f, 0f)

    addVertex(vertices[5]).setUv(0f, 1f).setNormal(0f, 0f, -1f)
    addVertex(vertices[4]).setUv(1f, 1f).setNormal(0f, 0f, -1f)
    addVertex(vertices[7]).setUv(1f, 0f).setNormal(0f, 0f, -1f)
    addVertex(vertices[6]).setUv(0f, 0f).setNormal(0f, 0f, -1f)

    addVertex(vertices[4]).setUv(0f, 1f).setNormal(1f, 0f, 0f)
    addVertex(vertices[0]).setUv(1f, 1f).setNormal(1f, 0f, 0f)
    addVertex(vertices[3]).setUv(1f, 0f).setNormal(1f, 0f, 0f)
    addVertex(vertices[7]).setUv(0f, 0f).setNormal(1f, 0f, 0f)

    addVertex(vertices[1]).setUv(0f, 1f).setNormal(0f, 1f, 0f)
    addVertex(vertices[0]).setUv(1f, 1f).setNormal(0f, 1f, 0f)
    addVertex(vertices[4]).setUv(1f, 0f).setNormal(0f, 1f, 0f)
    addVertex(vertices[5]).setUv(0f, 0f).setNormal(0f, 1f, 0f)

    addVertex(vertices[3]).setUv(0f, 1f).setNormal(0f, -1f, 0f)
    addVertex(vertices[2]).setUv(1f, 1f).setNormal(0f, -1f, 0f)
    addVertex(vertices[6]).setUv(1f, 0f).setNormal(0f, -1f, 0f)
    addVertex(vertices[7]).setUv(0f, 0f).setNormal(0f, -1f, 0f)
}