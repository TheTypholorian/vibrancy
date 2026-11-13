package net.typho.vibrancy

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier

object Vibrancy : ModInitializer {
    const val MOD_ID = "vibrancy"

    @JvmStatic
    fun id(path: String) = Identifier.fromNamespaceAndPath(MOD_ID, path)

    override fun onInitialize() {
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