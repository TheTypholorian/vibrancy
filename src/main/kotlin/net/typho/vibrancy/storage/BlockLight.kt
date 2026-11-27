package net.typho.vibrancy.storage

import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlRenderPass
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlIndexType
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.mixin.collection.DefaultChunkRendererAccessor
import net.typho.vibrancy.mixin.collection.RenderRegionAccessor
import net.typho.vibrancy.mixin.collection.SodiumWorldRendererAccessor
import net.typho.vibrancy.mixin.render.GlCommandEncoderAccessor
import net.typho.vibrancy.mixin.render.GlRenderPassAccessor
import net.typho.vibrancy.mixin.render.GlVertexArrayTessellationAccessor
import net.typho.vibrancy.mixin.render.RenderPassAccessor
import net.typho.vibrancy.util.MeshLayer
import net.typho.vibrancy.util.ShadowMap
import org.lwjgl.opengl.GL11.GL_BACK
import org.lwjgl.opengl.GL11.GL_FRONT
import org.lwjgl.opengl.GL11.glCullFace
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniform3f
import org.lwjgl.opengl.GL20.glUniform3i
import org.lwjgl.opengl.GL30.glUniform1ui
import org.lwjgl.opengl.GL32.nglMultiDrawElementsBaseVertex
import java.util.OptionalDouble
import java.util.OptionalInt
import kotlin.reflect.KMutableProperty0

open class BlockLight(
    @JvmField
    val pos: BlockPos
) : AutoCloseable {
    companion object {
        const val TEXTURE_SIZE = 144
        const val LIGHT_RADIUS = 16 // TODO

        // TODO make a better way to create a depth-only fbo that doesn't cause memory leaks
        @JvmField
        val dummyColorShadowMap = RenderSystem.getDevice().createTexture(
            { "Dummy point light shadow map (color)" },
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
    val shadowMap = ShadowMap(TEXTURE_SIZE * 6, TEXTURE_SIZE, "Point light shadow map at $pos")

    var solidLayer: MeshLayer? = null
        protected set(value) {
            field?.batch?.delete()
            field = value
        }
    var translucentLayer: MeshLayer? = null
        protected set(value) {
            field?.batch?.delete()
            field = value
        }

    @JvmField
    var collectDirty = true
    @JvmField
    var generateDirty = true
    var animatedTextures: Collection<TextureAtlasSprite> = listOf()
        protected set

    fun collect(): Boolean {
        val textures = mutableMapOf<Identifier, TextureAtlasSprite>()
        val transmissionAtlas = Minecraft.getInstance().atlasManager.getAtlasOrThrow(Vibrancy.transmissionAtlasId)
        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        val sections = mutableListOf<RenderSection>()

        for (x in ((pos.x - LIGHT_RADIUS) / 16)..((pos.x + LIGHT_RADIUS) / 16)) {
            for (y in ((pos.y - LIGHT_RADIUS) / 16)..((pos.y + LIGHT_RADIUS) / 16)) {
                for (z in ((pos.z - LIGHT_RADIUS) / 16)..((pos.z + LIGHT_RADIUS) / 16)) {
                    val region = sectionManager.regions.getForChunk(x, y, z) ?: continue
                    val section = (region as RenderRegionAccessor).`vibrancy$sections`()[LocalSectionIndex.pack(x, y, z)] ?: continue
                    sections.add(section)

                    region.getAnimatedSprites(section.sectionIndex)?.let { sprites ->
                        sprites.forEach { sprite ->
                            textures.computeIfAbsent(sprite.contents().name()) { transmissionAtlas.getSprite(it) }
                        }
                    }
                }
            }
        }

        animatedTextures = textures.values

        fun createLayer(property: KMutableProperty0<MeshLayer?>, vararg passes: TerrainRenderPass) {
            property.set(null)
            val layer by lazy {
                MeshLayer(MultiDrawBatch(sections.size * passes.size), sections.size * passes.size).also { property.set(it) }
            }

            if (sections.isNotEmpty()) {
                val region = sections.first().region // TODO region borders

                for (pass in passes) {
                    val storage = region.getStorage(pass) ?: continue

                    for (section in sections) {
                        if (section.region !== region) {
                            continue // TODO
                        }

                        val pMeshData = storage.getDataPointer(section.sectionIndex)

                        for (i in 0 until ModelQuadFacing.COUNT) {
                            if (SectionRenderDataUnsafe.getVertexCount(pMeshData, i) > 0) {
                                DefaultChunkRendererAccessor.`vibrancy$addSharedIndexedDrawCommands`(layer.batch, pMeshData, ModelQuadFacing.ALL)
                                break
                            }
                        }
                    }
                }
            }
        }

        createLayer(::solidLayer, DefaultTerrainRenderPasses.SOLID, DefaultTerrainRenderPasses.CUTOUT)
        createLayer(::translucentLayer, DefaultTerrainRenderPasses.TRANSLUCENT)

        return true
    }

    fun generateShadowMap(layer: MeshLayer, region: RenderRegion, colorTexture: GpuTextureView, depthTexture: GpuTextureView, indexedTessellation: Boolean, name: String): Boolean {
        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "Generate $name shadow map at $pos" },
            colorTexture,
            OptionalInt.of(0),
            depthTexture,
            OptionalDouble.of(1.0)
        ).use { pass ->
            pass.setPipeline(Vibrancy.pointLightShadowMapPipeline)
            pass.bindTexture(
                "u_BlockTex",
                @Suppress("DEPRECATION")
                Minecraft.getInstance().textureManager.getTexture(Vibrancy.transmissionAtlasLocation).textureView,
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

            val resources = region.resources ?: return false
            val tessellation = (if (indexedTessellation) resources.indexedTessellation else resources.tessellation) ?: return false
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

            glCullFace(GL_FRONT) // face winding order is inverted here for some reason

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
                    layer.batch.pElementCount,
                    GlIndexType.UNSIGNED_INT.formatId,
                    layer.batch.pElementPointer,
                    layer.batch.size,
                    layer.batch.pBaseVertex
                )
            }

            glCullFace(GL_BACK)
        }

        return true
    }

    fun generateShadowMap(): Boolean {
        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        val region = sectionManager.regions.getForChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4) ?: return false

        solidLayer?.let { layer -> generateShadowMap(layer, region, dummyColorShadowMapView, shadowMap.solidDepthView, false, "solid") }
        translucentLayer?.let { layer ->
            val texture = shadowMap.translucentLayerOrNew()
            generateShadowMap(layer, region, texture.colorView, texture.depthView, true, "translucent")
        }

        return true
    }

    fun render(pass: RenderPass, camera: CameraTransform): Boolean {
        val solidLayer = this@BlockLight.solidLayer ?: return false

        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        val region = sectionManager.regions.getForChunk(pos.x shr 4, pos.y shr 4, pos.z shr 4) ?: return false // TODO

        @Suppress("UnstableApiUsage")
        animatedTextures.forEach { SpriteUtil.INSTANCE.markSpriteActive(it) }

        val shadowSampler = RenderSystem.getSamplerCache().getSampler(
            AddressMode.CLAMP_TO_EDGE,
            AddressMode.CLAMP_TO_EDGE,
            FilterMode.LINEAR, // TODO
            FilterMode.LINEAR,
            true
        )
        pass.bindTexture("u_ShadowTex", shadowMap.solidDepthView, shadowSampler)

        val translucentTexture = shadowMap.translucentLayer

        if (translucentTexture != null) {
            pass.bindTexture("u_TranslucentColorTex", translucentTexture.colorView, shadowSampler)
            pass.bindTexture("u_TranslucentDepthTex", translucentTexture.depthView, shadowSampler)
        }

        val resources = region.resources ?: return false
        val tessellation = resources.tessellation ?: return false // TODO render on translucent
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
        glUniform1ui(
            glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_UseTranslucency"), // TODO
            if (translucentTexture == null) 0 else 1
        )
        GlStateManager._glBindVertexArray(tessellation.`vibrancy$array`().handle())
        nglMultiDrawElementsBaseVertex(
            tessellation.primitiveType.id,
            solidLayer.batch.pElementCount,
            GlIndexType.UNSIGNED_INT.formatId,
            solidLayer.batch.pElementPointer,
            solidLayer.batch.size,
            solidLayer.batch.pBaseVertex
        )

        return true
    }

    override fun close() {
        shadowMap.close()
        solidLayer?.batch?.delete()
    }
}