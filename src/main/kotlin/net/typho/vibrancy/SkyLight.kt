package net.typho.vibrancy

import com.ibm.icu.impl.ValidIdentifiers
import com.mojang.blaze3d.opengl.GlRenderPass
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlIndexType
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.typho.vibrancy.mixin.collection.SodiumWorldRendererAccessor
import net.typho.vibrancy.mixin.render.GlCommandEncoderAccessor
import net.typho.vibrancy.mixin.render.GlRenderPassAccessor
import net.typho.vibrancy.mixin.render.GlVertexArrayTessellationAccessor
import net.typho.vibrancy.mixin.render.RenderPassAccessor
import net.typho.vibrancy.util.ShadowMap
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.GL_BACK
import org.lwjgl.opengl.GL11.GL_FRONT
import org.lwjgl.opengl.GL11.glCullFace
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniform3f
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import org.lwjgl.opengl.GL32.nglMultiDrawElementsBaseVertex
import java.util.OptionalDouble
import java.util.OptionalInt

object SkyLight {
    const val TEXTURE_SIZE = 4096
    // TODO make a better way to create a depth-only fbo that doesn't cause memory leaks
    @JvmField
    val dummyColorShadowMap = RenderSystem.getDevice().createTexture(
        { "Dummy sky light shadow map (color)" },
        GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
        TextureFormat.RGBA8,
        TEXTURE_SIZE,
        TEXTURE_SIZE,
        1,
        1
    )
    @JvmField
    val dummyColorShadowMapView = RenderSystem.getDevice().createTextureView(dummyColorShadowMap)

    @JvmField
    val shadowMap = ShadowMap(TEXTURE_SIZE, TEXTURE_SIZE, "Sky light shadow map")

    fun generateShadowMap(
        camera: CameraTransform,
        sectionManager: RenderSectionManager,
        skyMatrix: Matrix4f,
        colorTexture: GpuTextureView,
        depthTexture: GpuTextureView,
        indexedTessellation: Boolean,
        name: String,
        vararg passes: TerrainRenderPass
    ) {
        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "Generate sky light $name shadow map" },
            colorTexture,
            OptionalInt.of(0),
            depthTexture,
            OptionalDouble.of(1.0)
        ).use { pass ->
            pass.setPipeline(Vibrancy.skyLightShadowMapPipeline)
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

            pass as RenderPassAccessor
            val backend = pass.`vibrancy$backend`()
            backend as GlRenderPass
            backend as GlRenderPassAccessor
            val encoder = backend.`vibrancy$encoder`()
            encoder as GlCommandEncoderAccessor

            if (!encoder.`vibrancy$trySetup`(backend, listOf())) {
                return
            }

            glUniformMatrix4fv(
                glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_SkyMat"), // TODO
                false,
                skyMatrix.get(FloatArray(16))
            )

            for (region in sectionManager.regions.loadedRegions) {
                val resources = region.resources ?: continue
                val tessellation = (if (indexedTessellation) resources.indexedTessellation else resources.tessellation) ?: continue
                tessellation as GlVertexArrayTessellationAccessor // TODO custom tessellation instances?
                GlStateManager._glBindVertexArray(tessellation.`vibrancy$array`().handle())

                glUniform3f(
                    glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_LightOffset"), // TODO
                    (region.originX - camera.intX).toFloat(),
                    (region.originY - camera.intY).toFloat(),
                    (region.originZ - camera.intZ).toFloat()
                )

                for (terrainRenderPass in passes) {
                    val batch = region.getCachedBatch(terrainRenderPass) ?: continue // TODO custom batch without

                    if (!batch.isEmpty) {
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
            }
        }
    }

    fun render(camera: CameraTransform, target: RenderTarget) {
        val skyMatrix = Matrix4f()
            .rotateX(Math.toRadians(-45.0).toFloat())
            .rotateY(Math.toRadians(-15.0).toFloat())
        val lightDirection = Vector4f(0f, 0f, -1f, 0f).mul(skyMatrix.invert(Matrix4f())) // why do we have to invert the matrix? we'll never know
        skyMatrix.scale(1f / 256) // TODO

        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        generateShadowMap(camera, sectionManager, skyMatrix, dummyColorShadowMapView, shadowMap.solidDepthView, false, "solid", DefaultTerrainRenderPasses.SOLID, DefaultTerrainRenderPasses.CUTOUT)
        val translucentTexture = shadowMap.translucentLayerOrNew()
        generateShadowMap(camera, sectionManager, skyMatrix, translucentTexture.colorView, translucentTexture.depthView, true, "translucent", DefaultTerrainRenderPasses.TRANSLUCENT)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "Render sky light" },
            target.colorTextureView!!,
            OptionalInt.empty(),
            target.depthTextureView!!,
            OptionalDouble.empty()
        ).use { pass ->
            pass.setPipeline(Vibrancy.skyLightRenderPipeline)
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

            val shadowSampler = RenderSystem.getSamplerCache().getSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, // TODO
                FilterMode.LINEAR,
                true
            )
            val translucentTexture = shadowMap.translucentLayerOrNew()
            pass.bindTexture("u_ShadowTex", shadowMap.solidDepthView, shadowSampler)
            pass.bindTexture("u_TranslucentColorTex", translucentTexture.colorView, shadowSampler)
            pass.bindTexture("u_TranslucentDepthTex", translucentTexture.depthView, shadowSampler)

            pass as RenderPassAccessor
            val backend = pass.`vibrancy$backend`()
            backend as GlRenderPass
            backend as GlRenderPassAccessor
            val encoder = backend.`vibrancy$encoder`()
            encoder as GlCommandEncoderAccessor

            if (!encoder.`vibrancy$trySetup`(backend, listOf())) {
                return
            }

            glUniformMatrix4fv(
                glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_SkyMat"), // TODO
                false,
                skyMatrix.get(FloatArray(16))
            )
            glUniform3f(
                glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_LightDir"), // TODO
                lightDirection.x,
                lightDirection.y,
                lightDirection.z
            )

            val passes = listOf(DefaultTerrainRenderPasses.SOLID, DefaultTerrainRenderPasses.CUTOUT)
            for (region in sectionManager.regions.loadedRegions) {
                val resources = region.resources ?: continue
                val tessellation = resources.tessellation ?: continue // TODO
                tessellation as GlVertexArrayTessellationAccessor // TODO custom tessellation instances?
                GlStateManager._glBindVertexArray(tessellation.`vibrancy$array`().handle())

                glUniform3f(
                    glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_WorldOffset"), // TODO
                    (region.originX - camera.intX) - camera.fracX,
                    (region.originY - camera.intY) - camera.fracY,
                    (region.originZ - camera.intZ) - camera.fracZ
                )
                glUniform3f(
                    glGetUniformLocation(encoder.`vibrancy$lastProgram`().programId, "u_LightOffset"), // TODO
                    (region.originX - camera.intX).toFloat(),
                    (region.originY - camera.intY).toFloat(),
                    (region.originZ - camera.intZ).toFloat()
                )

                for (terrainRenderPass in passes) {
                    val batch = region.getCachedBatch(terrainRenderPass) ?: continue

                    if (!batch.isEmpty) {
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
            }
        }
    }
}