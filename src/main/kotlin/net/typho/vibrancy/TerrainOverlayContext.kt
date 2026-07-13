package net.typho.vibrancy

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.textures.GpuSamplerImpl
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform
import net.caffeinemc.mods.sodium.client.util.FogParameters
import net.minecraft.client.renderer.RenderType
import net.typho.big_shot_lib.api.client.rendering.common.GpuBuffer
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjectName
import java.util.function.BiConsumer
import java.util.function.Function

interface TerrainOverlayContext {
    val matrices: ChunkRenderMatrices
    val terrainType: TerrainRenderPass
    val camera: CameraTransform
    val fog: FogParameters
    val terrainSampler: GpuSamplerImpl
    val globals: GpuBufferSlice
    val sectionTimeInfo: GpuBuffer
    val encoder: CommandEncoder

    fun pass(
        name: GpuObjectName,
        renderType: RenderType,
        out: Function<RenderPass, BiConsumer<ChunkRenderList, Runnable>>
    )

    fun keyedPass(
        name: GpuObjectName,
        passFunc: Function<ChunkRenderList, RenderType>,
        out: Function<RenderPass, BiConsumer<ChunkRenderList, Runnable>>
    )
}