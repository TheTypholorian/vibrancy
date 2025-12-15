package net.typho.vibrancy.util

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.IFramebuffer
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glClearColor
import java.awt.Color

fun Block.getKey(): ResourceKey<Block> = BuiltInRegistries.BLOCK.getResourceKey(this).orElseThrow()

fun Color.withBrightness(b: Float) = Color((red * b).toInt(), (green * b).toInt(), (blue * b).toInt())

fun IFramebuffer.glClear(mask: Int) {
    bind().use {
        glClearColor(0f, 0f, 0f, 0f)
        GL11.glClear(mask)
    }
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
