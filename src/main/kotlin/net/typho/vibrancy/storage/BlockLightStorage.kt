package net.typho.vibrancy.storage

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.mixin.collection.SodiumWorldRendererAccessor
import java.util.OptionalDouble
import java.util.OptionalInt
import kotlin.use

class BlockLightStorage(
    @JvmField
    val region: RenderRegion
) : AutoCloseable {
    companion object {
        @JvmStatic
        fun BlockState.isRaytracedLight() = lightEmission >= 8 && !`is`(Blocks.LAVA) // TODO
    }

    @JvmField
    val sections = Array(RenderRegion.REGION_SIZE) { Section() }

    fun onSectionChanged(pos: SectionPos) {
        val chunk = Minecraft.getInstance().level!!.getChunk(pos.x, pos.z)
        val chunkSection = chunk.getSection(chunk.getSectionIndexFromSectionY(pos.y))

        if (chunkSection.maybeHas { it.isRaytracedLight() }) {
            val section = getSection(pos)

            // might've added, removed, or not changed lights, so check them all
            val lights = mutableSetOf<BlockPos>()

            repeat(16) { x ->
                repeat(16) { y ->
                    repeat(16) { z ->
                        if (chunkSection.getBlockState(x, y, z).isRaytracedLight()) {
                            lights.add(BlockPos(x + pos.minBlockX(), y + pos.minBlockY(), z + pos.minBlockZ()))
                        }
                    }
                }
            }

            val old = section.lights.toMutableMap()
            section.lights.clear()

            for (pos in lights) {
                section.lights[pos] = old.remove(pos) ?: BlockLight(pos) // TODO check block state as well
            }

            old.forEach { (pos, light) -> light.close() }
        } else {
            getSection(pos).close()
        }

        for (x in (pos.x - 1).coerceAtLeast(region.chunkX)..(pos.x + 1).coerceAtMost(region.chunkX + RenderRegion.REGION_WIDTH - 1)) {
            for (y in (pos.y - 1).coerceAtLeast(region.chunkY)..(pos.y + 1).coerceAtMost(region.chunkY + RenderRegion.REGION_HEIGHT - 1)) {
                for (z in (pos.z - 1).coerceAtLeast(region.chunkZ)..(pos.z + 1).coerceAtMost(region.chunkZ + RenderRegion.REGION_LENGTH - 1)) {
                    val targetPos = SectionPos.of(x, y, z)
                    val section = getSection(targetPos)

                    for (light in section.lights.values) {
                        light.collectDirty = true
                        light.generateDirty = true
                    }
                }
            }
        }
    }

    fun getSection(pos: SectionPos) = sections[LocalSectionIndex.pack(pos.x, pos.y, pos.z)]

    override fun close() {
        sections.forEach { it.close() }
    }

    fun render() {
        sections.forEach {
            it.lights.values.forEach {
                if (it.collectDirty) {
                    it.collect()
                    it.collectDirty = false
                }

                if (it.generateDirty) {
                    it.generateShadowMap()
                    //it.generateDirty = false
                }
            }
        }

        val maxDrawBatchSize = sections.maxOf { it.lights.values.maxOfOrNull { it.drawBatch?.size ?: 0 } ?: 0 }

        if (maxDrawBatchSize == 0) {
            return
        }

        val cameraPos = Minecraft.getInstance().gameRenderer.mainCamera.position()
        val target = Minecraft.getInstance().mainRenderTarget

        val camera = CameraTransform(cameraPos.x, cameraPos.y, cameraPos.z)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "Vibrancy draw lights" },
            target.colorTextureView!!,
            OptionalInt.empty(),
            target.depthTextureView!!,
            OptionalDouble.empty()
        ).use { pass ->
            pass.setPipeline(Vibrancy.pointLightPipeline)
            pass.bindTexture(
                "u_BlockTex",
                @Suppress("DEPRECATION")
                Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).textureView,
                RenderSystem.getSamplerCache().getSampler(
                    AddressMode.CLAMP_TO_EDGE,
                    AddressMode.CLAMP_TO_EDGE,
                    FilterMode.NEAREST,
                    FilterMode.NEAREST,
                    true
                )
            )
            pass.setUniform(
                "u_Globals",
                (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor)
                    .`vibrancy$uniformBufferManager`()
                    .uniformBuffer
            )
            sections.forEach { it.lights.values.forEach { it.render(pass, camera) } }
        }
    }

    class Section : AutoCloseable {
        @JvmField
        val lights = mutableMapOf< BlockPos, BlockLight>()

        override fun close() {
            lights.values.forEach { it.close() }
            lights.clear()
        }
    }
}