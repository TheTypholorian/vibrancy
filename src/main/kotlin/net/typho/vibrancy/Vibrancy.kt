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
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeFormat
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatAttribute
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.typho.vibrancy.ext.RenderRegionExt
import net.typho.vibrancy.mixin.collection.SodiumWorldRendererAccessor

object Vibrancy : ClientModInitializer {
    const val MOD_ID = "vibrancy"

    @JvmStatic
    fun id(path: String) = Identifier.fromNamespaceAndPath(MOD_ID, path)

    @JvmField
    val transmissionAtlasLocation = id("textures/atlas/transmission.png")
    @JvmField
    val transmissionAtlasId = id("transmission")

    // used to bind sodium vertex attributes rather than vanilla attributes (in mixin.init.GlProgramMixin)
    @JvmField
    val chunkVertexShaders = setOf(
        id("point_light/shadow_map"),
        id("point_light/render"),
        id("sky_light/shadow_map"),
        id("sky_light/render")
    )
    @JvmField
    val normalVertexAttribute = VertexFormatAttribute("NORMAL", GlVertexAttributeFormat.UNSIGNED_BYTE, 3, true, false)

    @JvmField
    val pointLightShadowMapPipeline = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(id("point_light/shadow_map"))
            .withVertexShader(id("point_light/shadow_map"))
            .withFragmentShader(id("point_light/shadow_map"))
            .withShaderDefine("USE_VERTEX_COMPRESSION")
            .withSampler("u_BlockTex")
            .withUniform("u_Globals", UniformType.UNIFORM_BUFFER)
            .withCull(true)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .build()
    )
    @JvmField
    val pointLightRenderPipeline = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(id("point_light/render"))
            .withVertexShader(id("point_light/render"))
            .withFragmentShader(id("point_light/render"))
            .withShaderDefine("USE_VERTEX_COMPRESSION")
            .withSampler("u_BlockTex")
            .withSampler("u_ShadowTex")
            .withSampler("u_TranslucentColorTex")
            .withSampler("u_TranslucentDepthTex")
            .withUniform("u_Globals", UniformType.UNIFORM_BUFFER)
            .withCull(true)
            .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false, -1f, -1f))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .withColorTargetState(ColorTargetState(BlendFunction(SourceFactor.ONE, DestFactor.ONE)))
            .build()
    )

    @JvmField
    val skyLightShadowMapPipeline = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(id("sky_light/shadow_map"))
            .withVertexShader(id("sky_light/shadow_map"))
            .withFragmentShader(id("sky_light/shadow_map"))
            .withShaderDefine("USE_VERTEX_COMPRESSION")
            .withSampler("u_BlockTex")
            .withUniform("u_Globals", UniformType.UNIFORM_BUFFER)
            .withCull(true)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .build()
    )
    @JvmField
    val skyLightRenderPipeline = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(id("sky_light/render"))
            .withVertexShader(id("sky_light/render"))
            .withFragmentShader(id("sky_light/render"))
            .withShaderDefine("USE_VERTEX_COMPRESSION")
            .withSampler("u_BlockTex")
            .withSampler("u_ShadowTex")
            .withSampler("u_TranslucentColorTex")
            .withSampler("u_TranslucentDepthTex")
            .withUniform("u_Globals", UniformType.UNIFORM_BUFFER)
            .withCull(true)
            .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false, -1f, -1f))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .withColorTargetState(ColorTargetState(BlendFunction(SourceFactor.ONE, DestFactor.ONE)))
            .build()
    )

    @JvmStatic
    fun onSectionChanged(section: RenderSection) {
        (section.region as RenderRegionExt).`vibrancy$getBlockLightStorage`().onSectionChanged(section)
    }

    override fun onInitializeClient() {
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register {
            val cameraPos = Minecraft.getInstance().gameRenderer.mainCamera.position()
            val camera = CameraTransform(cameraPos.x, cameraPos.y, cameraPos.z)
            val target = Minecraft.getInstance().mainRenderTarget

            (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`().regions.loadedRegions.forEach { (it as RenderRegionExt).`vibrancy$getBlockLightStorage`().render(camera, target) }
            SkyLight.render(camera, target)
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