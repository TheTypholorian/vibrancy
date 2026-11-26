package net.typho.vibrancy.storage

import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlRenderPass
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlIndexType
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.core.BlockPos
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.mixin.collection.DefaultChunkRendererAccessor
import net.typho.vibrancy.mixin.collection.RenderRegionAccessor
import net.typho.vibrancy.mixin.collection.SodiumWorldRendererAccessor
import net.typho.vibrancy.mixin.render.GlCommandEncoderAccessor
import net.typho.vibrancy.mixin.render.GlRenderPassAccessor
import net.typho.vibrancy.mixin.render.GlVertexArrayTessellationAccessor
import net.typho.vibrancy.mixin.render.RenderPassAccessor
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniform3f
import org.lwjgl.opengl.GL20.glUniform3i
import org.lwjgl.opengl.GL30.glUniform1ui
import org.lwjgl.opengl.GL32.nglMultiDrawElementsBaseVertex
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

    var drawBatch: MultiDrawBatch? = null
        protected set(value) {
            field?.delete()
            field = value
        }
    var drawBatchCapacity = 0
        protected set

    @JvmField
    var collectDirty = true
    @JvmField
    var generateDirty = true

    fun collect(): Boolean {
        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        val sections = mutableListOf<RenderSection>()

        for (x in ((pos.x - LIGHT_RADIUS) / 16)..((pos.x + LIGHT_RADIUS) / 16)) {
            for (y in ((pos.y - LIGHT_RADIUS) / 16)..((pos.y + LIGHT_RADIUS) / 16)) {
                for (z in ((pos.z - LIGHT_RADIUS) / 16)..((pos.z + LIGHT_RADIUS) / 16)) {
                    val region = sectionManager.regions.getForChunk(x, y, z) ?: continue
                    val section = (region as RenderRegionAccessor).`vibrancy$sections`()[LocalSectionIndex.pack(x, y, z)] ?: continue
                    sections.add(section)
                }
            }
        }

        val batch = drawBatch?.let {
            if (drawBatchCapacity >= sections.size) {
                it
            } else {
                null
            }
        } ?: MultiDrawBatch(sections.size).also {
            drawBatch = it
            drawBatchCapacity = sections.size
        }
        batch.clear()

        if (sections.isEmpty()) {
            return true
        }

        val region = sections.first().region // TODO region borders
        val storage = region.getStorage(DefaultTerrainRenderPasses.SOLID /* TODO */) ?: return false

        for (section in sections) {
            if (section.region !== region) {
                continue // TODO
            }

            val pMeshData = storage.getDataPointer(section.sectionIndex)
            DefaultChunkRendererAccessor.`vibrancy$addSharedIndexedDrawCommands`(batch, pMeshData, ModelQuadFacing.ALL)
        }

        return true
    }

    fun generateShadowMap(): Boolean {
        val batch = drawBatch ?: return false

        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        val region = sectionManager.regions.getForChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4) ?: return false

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
            pass.setUniform(
                "u_Globals",
                (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor)
                    .`vibrancy$uniformBufferManager`()
                    .uniformBuffer
            )

            val tessellation = (region.resources ?: return false).tessellation ?: return false
            tessellation as GlVertexArrayTessellationAccessor // TODO custom tessellation instances?

            pass as RenderPassAccessor
            val backend = pass.`vibrancy$backend`()
            backend as GlRenderPass
            backend as GlRenderPassAccessor
            val encoder = backend.`vibrancy$encoder`()
            encoder as GlCommandEncoderAccessor

            if (!encoder.`vibrancy$trySetup`(backend, listOf())) {
                return false
            }

            glUniform3i(
                glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_RelativeOffset"), // TODO
                region.originX - pos.x,
                region.originY - pos.y,
                region.originZ - pos.z
            )
            GlStateManager._glBindVertexArray(tessellation.`vibrancy$array`().handle())

            val faceLocation = glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_Face") // TODO

            repeat(6) { face ->
                glUniform1ui(faceLocation, face)
                nglMultiDrawElementsBaseVertex(
                    tessellation.primitiveType.id,
                    batch.pElementCount,
                    GlIndexType.UNSIGNED_INT.formatId,
                    batch.pElementPointer,
                    batch.size,
                    batch.pBaseVertex
                )
            }
        }

        return true
    }

    fun render(pass: RenderPass, camera: CameraTransform): Boolean {
        val batch = drawBatch ?: return false

        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        val region = sectionManager.regions.getForChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4) ?: return false

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

        val tessellation = (region.resources ?: return false).tessellation ?: return false
        tessellation as GlVertexArrayTessellationAccessor // TODO custom tessellation instances?

        pass as RenderPassAccessor
        val backend = pass.`vibrancy$backend`()
        backend as GlRenderPass
        backend as GlRenderPassAccessor
        val encoder = backend.`vibrancy$encoder`()
        encoder as GlCommandEncoderAccessor

        if (!encoder.`vibrancy$trySetup`(backend, listOf())) {
            return false
        }

        glUniform3f(
            glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_WorldOffset"), // TODO
            (region.originX - camera.intX) - camera.fracX,
            (region.originY - camera.intY) - camera.fracY,
            (region.originZ - camera.intZ) - camera.fracZ
        )
        glUniform3i(
            glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_RelativeOffset"), // TODO
            region.originX - pos.x,
            region.originY - pos.y,
            region.originZ - pos.z
        )
        GlStateManager._glBindVertexArray(tessellation.`vibrancy$array`().handle())
        nglMultiDrawElementsBaseVertex(
            tessellation.primitiveType.id,
            batch.pElementCount,
            GlIndexType.UNSIGNED_INT.formatId,
            batch.pElementPointer,
            batch.size,
            batch.pBaseVertex
        )

        return true
    }

    override fun close() {
        shadowMapView.close()
        shadowMap.close()
        drawBatch?.delete()
    }
}