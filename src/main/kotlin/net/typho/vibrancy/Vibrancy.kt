package net.typho.vibrancy

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.typho.vibrancy.collection.VibrancyChunkVertexType
import net.typho.vibrancy.ext.RenderRegionExt
import net.typho.vibrancy.mixin.collection.SodiumWorldRendererAccessor

object Vibrancy : ClientModInitializer {
    const val MOD_ID = "vibrancy"
    var chunkBuildContext: ChunkBuildContext? = null // TODO multithreading (?)
        private set

    @JvmStatic
    fun id(path: String) = Identifier.fromNamespaceAndPath(MOD_ID, path)

    @JvmField
    val shadowMapPipeline = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(id("point_light/shadow_map"))
            .withVertexShader(id("point_light/shadow_map"))
            .withFragmentShader(id("point_light/shadow_map"))
            .withSampler("u_BlockTex")
            .withCull(false)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .build()
    )
    @JvmField
    val pointLightPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("point_light/render"))
            .withVertexShader(id("point_light/render"))
            .withFragmentShader(id("point_light/render"))
            .withSampler("u_BlockTex")
            .withSampler("u_ShadowTex")
            .withUniform("u_LightInfo", UniformType.UNIFORM_BUFFER)
            .withCull(true)
            .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false, -1f, -1f))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .withColorTargetState(ColorTargetState(BlendFunction(SourceFactor.ONE, DestFactor.ONE)))
            .build()
    )

    override fun onInitializeClient() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { minecraft, level ->
            chunkBuildContext?.cleanup()
            chunkBuildContext = ChunkBuildContext(level, VibrancyChunkVertexType)
        }
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register {
            (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`().regions.loadedRegions.forEach { (it as RenderRegionExt).`vibrancy$getBlockLightStorage`().render() }
        }
        /*
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, context ->
            dispatcher.register(literal("vibrancy").executes { source ->

                1
            })
        }
         */
    }

    /*
    @JvmField
    val raytracedPointRenderTypes = Util.memoize { key: RayPointLightRenderTypeKey ->
        GpuObjects.renderType(
            key.location,
            CompactChunkVertex.VERTEX_FORMAT,
            GpuDrawSettings.Builder()
                .blend(GpuBlendFunction.ADDITIVE)
                .shader(id("raytraced_point"))
                .cull()
                .depth(GpuAlphaFunction.gequal)
                .writeDepth(false)
                .zOffset()
                .sampler("u_BlockTex")
                .sampler("u_MaterialTex")
                .sampler("u_TransmissionTex")
                .uniform("Globals")
                .uniform("u_Globals")
                .uniform("u_VibrancyConfig")
                .storageBuffer("u_Lights")
                .storageBuffer("u_Shadows")
                .storageBuffer("u_Grids")
                .texelBuffer("u_SectionTimeInfo", GpuDataType.sint32, 1)
                .define("USE_VERTEX_COMPRESSION")
                .define("USE_FOG")
                .apply {
                    if (key.alignPixels) {
                        define("ALIGN_PIXELS")
                    }

                    if (key.testQuads) {
                        define("TEST_QUADS")
                    }

                    define("BRIGHTNESS", key.brightness)

                    if (key.raycastLightModel) {
                        define("RAYCAST_LIGHT_MODEL")
                    }

                    if (key.reflectionsEnabled) {
                        define("REFLECTIONS_ENABLED")
                    }

                    define("REFLECTION_STRENGTH", key.reflectionStrength)

                    if (key.limitBrightness) {
                        define("LIMIT_BRIGHTNESS")
                    }
                },
            RenderType.SMALL_BUFFER_SIZE,
            false,
            true,
            false
        )
    }

    @JvmField
    @Suppress("DEPRECATION")
    val entityShadowFormat = VertexFormat.builder(0)
        .position()
        .textureUV()
        .attribute("Color", GpuDataType.unorm8, 4, 12) // 8 bytes of padding cus SSBOs
        .build() // TODO fix NeoVertexFormats problem

    @JvmField
    val entityShadowRenderType = GpuObjects.renderType(
        id("entity_shadow"),
        DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR,
        GpuDrawSettings.Builder()
            .blend(GpuBlendFunction.ADDITIVE)
            .shader(id("entity_shadow"))
            .cull()
            .depth(GpuAlphaFunction.gequal)
            .writeDepth(false)
            .zOffset()
            .sampler("u_BlockTex")
            .sampler("u_TransmissionTex")
            .uniform("u_ShadowRange")
            .storageBuffer("u_Shadows")
            .uniform("DynamicTransforms")
            .uniform("Projection")
            .uniform("u_VibrancyConfig"),
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        false
    )
    val blitVertexBuffer by lazy {
        GpuObjects.buffer(
            { "Vibrancy Blit Vertex Buffer" },
            20L * 6,
            GpuBufferUsage.VERTEX
        ) { output ->
            fun vertex(x: Float, y: Float) {
                output.writeFloat(x * 2 - 1)
                output.writeFloat(y * 2 - 1)
                output.writeFloat(0f)
                output.writeFloat(x)
                output.writeFloat(y)
            }

            vertex(0f, 0f)
            vertex(1f, 0f)
            vertex(1f, 1f)

            vertex(1f, 1f)
            vertex(0f, 1f)
            vertex(0f, 0f)
        }
    }
    @JvmField
    val entityShadowBlitRenderType = GpuObjects.renderType(
        id("entity_shadow_blit"),
        DefaultVertexFormat.POSITION_TEX,
        GpuDrawSettings.Builder()
            .blend(GpuBlendFunction.TRANSLUCENT)
            .shader(id("entity_shadow_blit"))
            .depth(GpuAlphaFunction.always)
            .writeDepth(false)
            .sampler("u_ShadowTex")
            .uniform("u_VibrancyConfig"),
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        false
    )
     */
}