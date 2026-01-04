package net.typho.vibrancy.util

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import org.joml.Vector3f

fun Block.getKey(): ResourceKey<Block> = BuiltInRegistries.BLOCK.getResourceKey(this).orElseThrow()

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
