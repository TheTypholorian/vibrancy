package net.typho.vibrancy

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken
import net.caffeinemc.mods.sodium.client.world.LevelSlice
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.LayeringTransform
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import net.typho.vibrancy.mixin.RenderRegionAccessor
import net.typho.vibrancy.mixin.RenderSectionManagerAccessor
import net.typho.vibrancy.mixin.SodiumWorldRendererAccessor
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.OptionalDouble
import java.util.OptionalInt

object Vibrancy : ClientModInitializer {
    const val MOD_ID = "vibrancy"

    @JvmStatic
    fun id(path: String) = Identifier.fromNamespaceAndPath(MOD_ID, path)

    @JvmField
    val shadowMapPipeline = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(id("point_light/shadow_map"))
            .withVertexShader(id("point_light/shadow_map"))
            .withFragmentShader(id("point_light/shadow_map"))
            .withSampler("u_BlockTex")
            .withCull(false) // TODO
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
            .withCull(true)
            .withDepthStencilState(DepthStencilState(DepthStencilState.DEFAULT.depthTest, false))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .withColorTargetState(ColorTargetState(BlendFunction(SourceFactor.ONE, DestFactor.ONE)))
            .build()
    )
    private val colorTexture by lazy {
        RenderSystem.getDevice().createTexture(
            "Vibrancy test shadow map (color)",
            GpuTexture.USAGE_COPY_SRC or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.RGBA8,
            144 * 6,
            144,
            1,
            1
        )
    }
    private val depthTexture by lazy {
        RenderSystem.getDevice().createTexture(
            "Vibrancy test shadow map (depth)",
            GpuTexture.USAGE_COPY_SRC or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.DEPTH32,
            144 * 6,
            144,
            1,
            1
        )
    }
    private val lightPos = BlockPos(13, -60, 12)
    private val context by lazy { // TODO
        ChunkBuildContext(Minecraft.getInstance().level!!, VibrancyChunkVertexType(lightPos))
    }
    const val LIGHT_RADIUS = 6
    var vertexBuffer: GpuBuffer? = null
    var vertexCount = 0

    fun capture() {
        val cameraPos = Minecraft.getInstance().gameRenderer.mainCamera.position()
        val sectionManager = (SodiumWorldRenderer.instance() as SodiumWorldRendererAccessor).`vibrancy$renderSectionManager`()
        val results = mutableListOf<ChunkBuildOutput>()

        for (x in ((lightPos.x - LIGHT_RADIUS) / 16)..((lightPos.x + LIGHT_RADIUS) / 16)) {
            for (y in ((lightPos.y - LIGHT_RADIUS) / 16)..((lightPos.y + LIGHT_RADIUS) / 16)) {
                for (z in ((lightPos.z - LIGHT_RADIUS) / 16)..((lightPos.z + LIGHT_RADIUS) / 16)) {
                    val sectionPos = SectionPos.of(x, y, z)
                    val region = sectionManager.regions.getForChunk(sectionPos.x, sectionPos.y, sectionPos.z) ?: continue
                    val section = (region as RenderRegionAccessor).`vibrancy$sections`()[LocalSectionIndex.pack(sectionPos.x, sectionPos.y, sectionPos.z)]
                    val task = VibrancyChunkMeshingTask(
                        section,
                        0,
                        Vector3d(cameraPos.x, cameraPos.y, cameraPos.z),
                        LevelSlice.prepare(Minecraft.getInstance().level!!, sectionPos, (sectionManager as RenderSectionManagerAccessor).`vibrancy$sectionCache`()) ?: continue,
                        SortBehavior.OFF,
                        forceSort = false,
                        blockingTask = true,
                        lightPos
                    )

                    results.add(task.execute(context, CancellationToken.NEVER_CANCELLED))
                }
            }
        }

        val size = results.sumOf { it.meshes.values.sumOf { it.vertexData.length } }

        if (size == 0) {
            results.forEach { it.meshes.values.forEach { it.vertexData.free() } }
            return
        }

        vertexCount = size / VibrancyChunkVertexType.format.stride
        val indexCount = vertexCount / 4 * 6
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

        val indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)
        val indexBuffer = indices.getBuffer(indexCount)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "Vibrancy test shadow map pass" },
            RenderSystem.getDevice().createTextureView(colorTexture),
            OptionalInt.of(0),
            RenderSystem.getDevice().createTextureView(depthTexture),
            OptionalDouble.of(1.0)
        ).use { pass ->
            pass.setPipeline(shadowMapPipeline)
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
            pass.setVertexBuffer(0, vertexBuffer)
            pass.setIndexBuffer(indexBuffer, indices.type())
            pass.drawIndexed(0, 0, indexCount, 6)
        }

        val outDir = Minecraft.getInstance().gameDirectory.toPath().resolve("vibrancy")
        Files.createDirectories(outDir)

        TextureUtil.writeAsPNG(
            outDir,
            "shadow_map_color",
            colorTexture,
            0,
            { it }
        )
        TextureUtil.writeAsPNG(
            outDir,
            "shadow_map_depth",
            depthTexture,
            0,
            { it }
        )

        results.forEach { it.meshes.values.forEach { it.vertexData.free() } }
    }

    fun display() {
        val cameraPos = Minecraft.getInstance().gameRenderer.mainCamera.position()

        val vertexBuffer = vertexBuffer ?: return
        val indexCount = vertexCount / 4 * 6
        val indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)
        val indexBuffer = indices.getBuffer(indexCount)
        val target = Minecraft.getInstance().mainRenderTarget

        val stack = RenderSystem.getModelViewStack()
        stack.pushMatrix()
        LayeringTransform.VIEW_OFFSET_Z_LAYERING.modifier!!.accept(stack)
        stack.translate(Vec3(lightPos).toVector3f().sub(cameraPos.toVector3f()))

        val transforms = RenderSystem.getDynamicUniforms().writeTransform(
            stack,
            Vector4f(1f),
            Vector3f(),
            Matrix4f()
        )

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "Vibrancy test draw light" },
            target.colorTextureView!!,
            OptionalInt.empty(),
            target.depthTextureView!!,
            OptionalDouble.empty()
        ).use { pass ->
            pass.setPipeline(pointLightPipeline)
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
            pass.bindTexture(
                "u_ShadowTex",
                RenderSystem.getDevice().createTextureView(depthTexture), // TODO
                RenderSystem.getSamplerCache().getSampler(
                    AddressMode.CLAMP_TO_EDGE,
                    AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, // TODO
                    FilterMode.LINEAR,
                    true
                )
            )
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transforms)
            pass.setVertexBuffer(0, vertexBuffer)
            pass.setIndexBuffer(indexBuffer, indices.type())
            pass.drawIndexed(0, 0, indexCount, 1)
        }

        stack.popMatrix()
    }

    override fun onInitializeClient() {
        LevelRenderEvents.START_MAIN.register {
            capture()
        }
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register {
            display()
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