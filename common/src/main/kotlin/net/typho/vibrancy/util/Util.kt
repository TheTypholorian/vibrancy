package net.typho.vibrancy.util

import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
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

fun BlockBox.expand(v: Int) = BlockBox(
    BlockPos(min.x - v, min.y - v, min.z - v),
    BlockPos(min.x + v, min.y + v, min.z + v)
)

fun TextureAtlasSprite.matches(other: TextureAtlasSprite): Boolean {
    return atlasLocation() == other.atlasLocation() && u0 == other.u0 && u1 == other.u1 && v0 == other.v0 && v1 == other.v1
}