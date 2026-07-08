package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuBufferUsage
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.TerrainOverlayContext
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.mixin.TerrainRenderPassAccessor
import net.typho.vibrancy.util.TextureLayers
import java.util.function.BiConsumer

object RayPointLightType : BlockLightType<RayPointLightInfo, RayPointLightStorage> {
    @JvmStatic
    val emptyShadowBuffer by lazy { GpuObjects.buffer({ "Vibrancy Empty Shadow Buffer" }, 16L, GpuBufferUsage.SHADER_STORAGE) }

    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = RayPointLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): RayPointLightInfo? {
        return info as? RayPointLightInfo
    }

    override fun createStorage(manager: LightManager) = RayPointLightStorage()

    @Suppress("DEPRECATION")
    override fun render(
        manager: LightManager,
        context: TerrainOverlayContext,
        lights: RayPointLightStorage,
        debugOut: (key: String, value: Int) -> Unit,
        profiler: ProfilerFiller
    ) {
        if (VibrancyConfig.rayLightsEnabled) {
            // get atlases here because they might create render passes and vulkan doesn't like render pass inception
            val materialTex = TextureLayers.getMaterialOrThrow(TextureAtlas.LOCATION_BLOCKS).getTextureView()
            val transmissionTex = TextureLayers.getTransmissionOrThrow(TextureAtlas.LOCATION_BLOCKS).getTextureView()
            val configBuffer = VibrancyConfig.loadConfigBuffer()

            context.pass(
                { "Vibrancy Raytraced Point Lights for ${(context.terrainType as TerrainRenderPassAccessor).`vibrancy$getRenderType`()}" },
                Vibrancy.raytracedPointRenderType
            ) { pass ->
                pass.setUniform("Globals", RenderSystem.getGlobalSettingsUniform()!!)
                pass.setUniform("u_Globals", context.globals)
                pass.setUniform("u_VibrancyConfig", configBuffer)
                pass.setUniform("u_SectionTimeInfo", context.sectionTimeInfo)
                pass.bindTexture("u_BlockTex", context.terrainType.atlas, context.terrainSampler)
                pass.bindTexture("u_MaterialTex", materialTex, context.terrainSampler)
                pass.bindTexture("u_TransmissionTex", transmissionTex, context.terrainSampler)

                BiConsumer { renderList, draw ->
                    val storage = renderList.region.getStorage(context.terrainType)

                    if (storage != null) {
                        val regionData = lights.getOrPackRegion(renderList.region, manager)

                        if (regionData != null) {
                            pass.setUniform("u_Lights", regionData.lightBuffer)

                            if (regionData.shadowBuffer == null) {
                                pass.setUniform("u_Shadows", emptyShadowBuffer)
                            } else {
                                pass.setUniform("u_Shadows", regionData.shadowBuffer)
                            }

                            pass.setUniform("u_Grids", regionData.gridBuffer)
                            draw.run()
                        }
                    }
                }
            }
        }
    }

    /*
    override fun render(
        manager: LightManager,
        result: GlFramebuffer,
        temp: GlFramebuffer,
        data: RenderEventData,
        lights: HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>,
        debugOut: (String, Int) -> Unit,
        profiler: ProfilerFiller
    ) {
        if (VibrancyConfig.rayLightsEnabled) {
            synchronized(lights.map) {
                profiler.push("cull")
                val lights = lights.map.values
                    /*
                    .filter { light ->
                        manager.testFrustum(light.pos, data, light.boundingBox) && light.sections.any { manager.isSectionVisible(it) }
                    }
                     */
                    .map { light -> light to manager.getSortingOrder(data, light.pos) }
                    .sortedBy { it.second }
                    .take(VibrancyConfig.rayLightsMaxRendered)
                    .toList()
                profiler.pop()

                synchronized(manager.sectionLock) {
                    profiler.push("update")
                    val entityShadowDistance = manager.getRenderDistance(VibrancyConfig.entityShadowDistance)
                    lights.forEachIndexed { index, light ->
                        if (
                            light.first.sections.all { pos -> // TODO
                                manager.sectionMeshCaches.containsKey(pos)
                                        || data.level!!.getChunk(pos.x(), pos.z()).let { it.getSection(it.getSectionIndexFromSectionY(pos.y())) }.hasOnlyAir()
                                        || !manager.isSectionVisible(pos)
                            }
                        ) {
                            light.first.update(
                                data,
                                manager,
                                debugOut,
                                (VibrancyConfig.entityShadowsEnabled || VibrancyConfig.blockEntityShadows) && index < VibrancyConfig.entityShadowMaxBlockLights && light.second < entityShadowDistance,
                                profiler
                            )
                        }
                    }
                    profiler.pop()
                }

                profiler.push("draw")
                LightMesh.drawState(NeoAtlas.blocks, Vibrancy.id("block/raytraced/mesh")).bind().use { settings ->
                    profiler.push("uniforms")
                    settings.shader.setTexture(3, GlTextureBinding.FromInstance(
                        ReflectionAtlases[Identifier("blocks")], //NeoAtlas.blocks.location
                        GlTextureTarget.TEXTURE_2D
                    ))
                    settings.shader.setUniform("ProjMat") { set(data.projMat) }
                    settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat) }
                    FogUtil.INSTANCE.upload(settings.shader)
                    profiler.pop()

                    val highQualityDistance = 2f * 2f * 16f * 16f
                    val lights = lights.filter { it.first.sections.any { pos -> manager.isSectionVisible(pos) } }
                    val atlas = NeoAtlas.blocks

                    temp.bind().use { fbo ->
                        lights.forEachIndexed { index, light ->
                            if (light.second < highQualityDistance && index <= VibrancyConfig.rayLightMaxHighQuality) {
                                profiler.push("clear")
                                fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))
                                profiler.pop()

                                profiler.push("render")
                                light.first.render(data, settings.shader, atlas, debugOut, profiler)
                                profiler.pop()

                                profiler.push("blit")
                                manager.blitFromTemp(result, temp)
                                profiler.pop()
                            }
                        }
                    }

                    GlBlendShard.Enabled(
                        BlendFunction.Basic(
                            GlBlendingFactor.ONE,
                            GlBlendingFactor.ONE
                        ),
                        if (VibrancyConfig.limitLightBrightness) GlBlendEquation.MAX else GlBlendEquation.ADD
                    ).bind().use {
                        result.bind().use { fbo ->
                            lights.forEachIndexed { index, light ->
                                if (light.second >= highQualityDistance || index > VibrancyConfig.rayLightMaxHighQuality) {
                                    profiler.push("render")
                                    light.first.render(data, settings.shader, atlas, debugOut, profiler)
                                    profiler.pop()
                                }
                            }
                        }
                    }
                }
                profiler.pop()
            }
        }
    }
     */
}