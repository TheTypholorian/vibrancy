package net.typho.vibrancy.storage

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken
import net.caffeinemc.mods.sodium.client.world.LevelSlice
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.collection.VibrancyChunkMeshingTask
import net.typho.vibrancy.collection.VibrancyChunkVertexType
import net.typho.vibrancy.mixin.collection.RenderRegionAccessor
import net.typho.vibrancy.mixin.collection.RenderSectionManagerAccessor
import net.typho.vibrancy.mixin.collection.SodiumWorldRendererAccessor
import org.joml.Vector3d
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.OptionalDouble
import java.util.OptionalInt

open class BlockLight(
    @JvmField
    val pos: BlockPos
) : AutoCloseable {
    companion object {
        const val TEXTURE_SIZE = 144
        const val LIGHT_RADIUS = 16 // TODO

        // TODO make a better way to create a depth-only fbo
        @JvmField
        val dummyColorShadowMap = RenderSystem.getDevice().createTexture(
            { "Vibrancy shadow map color" },
            GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.RGBA8,
            TEXTURE_SIZE * 6,
            TEXTURE_SIZE,
            1,
            1
        )
        @JvmField
        val dummyColorShadowMapView = RenderSystem.getDevice().createTextureView(dummyColorShadowMap)
    }

    @JvmField
    val shadowMap = RenderSystem.getDevice().createTexture(
        { "Vibrancy shadow map for light at $pos" },
        GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
        TextureFormat.DEPTH32,
        TEXTURE_SIZE * 6,
        TEXTURE_SIZE,
        1,
        1
    )
    @JvmField
    val shadowMapView = RenderSystem.getDevice().createTextureView(shadowMap)
    @JvmField
    val infoBuffer = RenderSystem.getDevice().createBuffer(
        { "Vibrancy light info buffer for light at pos $pos" },
        GpuBuffer.USAGE_UNIFORM,
        ByteBuffer.allocateDirect(4 * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .putInt(pos.x)
            .putInt(pos.y)
            .putInt(pos.z)
            .putInt(0)
            .flip()
    )

    var vertexBuffer: GpuBuffer? = null
        protected set
    var vertexCount = 0
        protected set
    @JvmField
    var collectDirty = true
    @JvmField
    var generateDirty = true

    fun collect() {
        val cameraPos = Minecraft.getInstance().gameRenderer.mainCamera.position()
        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        val results = mutableListOf<ChunkBuildOutput>()
        VibrancyChunkVertexType.lightPos = pos

        for (x in ((pos.x - LIGHT_RADIUS) / 16)..((pos.x + LIGHT_RADIUS) / 16)) {
            for (y in ((pos.y - LIGHT_RADIUS) / 16)..((pos.y + LIGHT_RADIUS) / 16)) {
                for (z in ((pos.z - LIGHT_RADIUS) / 16)..((pos.z + LIGHT_RADIUS) / 16)) {
                    val sectionPos = SectionPos.of(x, y, z)
                    val region = sectionManager.regions.getForChunk(sectionPos.x, sectionPos.y, sectionPos.z) ?: continue
                    val section = (region as RenderRegionAccessor).`vibrancy$sections`()[LocalSectionIndex.pack(sectionPos.x, sectionPos.y, sectionPos.z)] ?: continue
                    val task = VibrancyChunkMeshingTask(
                        section,
                        0,
                        Vector3d(cameraPos.x, cameraPos.y, cameraPos.z),
                        LevelSlice.prepare( // TODO
                            Minecraft.getInstance().level!!,
                            sectionPos,
                            (sectionManager as RenderSectionManagerAccessor).`vibrancy$sectionCache`()
                        ) ?: continue,
                        SortBehavior.OFF,
                        forceSort = false,
                        blockingTask = true,
                        pos
                    )

                    results.add(task.execute(Vibrancy.chunkBuildContext!!, CancellationToken.NEVER_CANCELLED))
                }
            }
        }

        val size = results.sumOf { it.meshes.values.sumOf { it.vertexData.length } }

        if (size == 0) {
            results.forEach { it.meshes.values.forEach { it.vertexData.free() } }
            return
        }

        vertexCount = size / VibrancyChunkVertexType.format.stride
        val vertexByteBuffer = ByteBuffer.allocateDirect(size)

        // TODO
        for (result in results) {
            for (parts in result.meshes.values) {
                vertexByteBuffer.put(parts.vertexData.directBuffer)
            }
        }

        val vertexBuffer = vertexBuffer?.let {
            if (it.size() >= size) it else null
        } ?: RenderSystem.getDevice().createBuffer(
            null,
            GpuBuffer.USAGE_COPY_DST or GpuBuffer.USAGE_VERTEX,
            size.toLong()
        ).also {
            vertexBuffer?.close()
            vertexBuffer = it
        }

        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(vertexBuffer.slice(), vertexByteBuffer.flip())

        results.forEach { it.meshes.values.forEach { it.vertexData.free() } }
    }

    fun generateShadowMap(): Boolean {
        val vertexBuffer = vertexBuffer ?: return false

        val indexCount = vertexCount / 4 * 6
        val indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)
        val indexBuffer = indices.getBuffer(indexCount)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "Vibrancy generate shadow map for light at pos $pos" },
            dummyColorShadowMapView,
            OptionalInt.of(0), // TODO
            shadowMapView,
            OptionalDouble.of(1.0)
        ).use { pass ->
            pass.setPipeline(Vibrancy.shadowMapPipeline)
            pass.bindTexture(
                "u_BlockTex",
                @Suppress("DEPRECATION")
                Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).textureView,
                RenderSystem.getSamplerCache().getSampler(
                    AddressMode.CLAMP_TO_EDGE,
                    AddressMode.CLAMP_TO_EDGE,
                    FilterMode.NEAREST,
                    FilterMode.NEAREST,
                    false
                )
            )
            pass.setVertexBuffer(0, vertexBuffer)
            pass.setIndexBuffer(indexBuffer, indices.type())
            pass.drawIndexed(0, 0, indexCount, 6)
        }

        return true
    }

    fun render(pass: RenderPass): Boolean {
        val vertexBuffer = vertexBuffer ?: return false
        val indexCount = vertexCount / 4 * 6

        pass.bindTexture(
            "u_ShadowTex",
            shadowMapView,
            RenderSystem.getSamplerCache().getSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, // TODO
                FilterMode.LINEAR,
                true
            )
        )
        pass.setUniform("u_LightInfo", infoBuffer)
        pass.setVertexBuffer(0, vertexBuffer)
        pass.drawIndexed(0, 0, indexCount, 1)

        return true
    }

    override fun close() {
        shadowMapView.close()
        shadowMap.close()
        vertexBuffer?.close()
        infoBuffer.close()
    }
}