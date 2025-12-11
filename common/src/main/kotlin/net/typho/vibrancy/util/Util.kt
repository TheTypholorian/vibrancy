package net.typho.vibrancy.util

import com.mojang.blaze3d.vertex.VertexConsumer
import foundry.veil.api.client.color.Color
import foundry.veil.api.client.color.Colorc
import foundry.veil.api.client.render.framebuffer.AdvancedFbo
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glClearColor

fun Block.getKey(): ResourceKey<Block> = BuiltInRegistries.BLOCK.getResourceKey(this).orElseThrow()

fun Colorc.withBrightness(b: Float) = Color(red() * b, green() * b, blue() * b)

fun AdvancedFbo.glClear(mask: Int) {
    bind(true)
    glClearColor(0f, 0f, 0f, 0f)
    GL11.glClear(mask)
    AdvancedFbo.unbind()
}

fun boxOfRadius(center: Vector3f, radius: Float) = AABB(
    (center.x - radius).toDouble(),
    (center.y - radius).toDouble(),
    (center.z - radius).toDouble(),
    (center.x + radius).toDouble(),
    (center.y + radius).toDouble(),
    (center.z + radius).toDouble()
)

fun BlockBox.expand(v: Int) = BlockBox(
    BlockPos(min.x - v, min.y - v, min.z - v),
    BlockPos(min.x + v, min.y + v, min.z + v)
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