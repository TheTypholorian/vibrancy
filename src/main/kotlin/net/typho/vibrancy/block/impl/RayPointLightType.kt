package net.typho.vibrancy.block.impl

import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendEquation
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendingFactor
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlClearBit
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlBlendShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.util.FogUtil
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.util.ReflectionAtlases

object RayPointLightType : BlockLightType<RayPointLightInfo, HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>> {
    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = RayPointLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): RayPointLightInfo? {
        return info as? RayPointLightInfo
    }

    override fun createStorage(manager: LightManager) = RayPointLightStorage()

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
}