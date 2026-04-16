package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.util.ReflectionAtlases
import org.joml.Matrix4f

object RayPointLightType : BlockLightType<RayPointLightInfo, HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>> {
    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = RayPointLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): RayPointLightInfo? {
        return info as? RayPointLightInfo
    }

    override fun createStorage(manager: LightManager) = RayPointLightStorage()

    override fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>,
        debugOut: (String, Int) -> Unit
    ) {
        if (VibrancyConfig.rayLightsEnabled) {
            synchronized(lights.map) {
                val lights = lights.map.values
                    .filter { light ->
                        data.frustum.testAab(
                            (light.boundingBox.min.toFloat() - data.camera.pos).toJOML(),
                            (light.boundingBox.max.toFloat() - data.camera.pos).toJOML(),
                        )
                    }
                    .map { light -> light to manager.getSortingOrder(data, light.pos) }
                    .sortedBy { it.second }
                    .take(VibrancyConfig.rayLightsMaxRendered)
                    .toList()

                LightMesh.drawState(NeoAtlas.blocks, Vibrancy.id("block/raytraced/mesh")).bind().use { settings ->
                    settings.shader.setTexture(3, GlTextureBinding.FromInstance(
                        ReflectionAtlases[NeoIdentifier("blocks")], //NeoAtlas.blocks.location
                        GlTextureTarget.TEXTURE_2D
                    ))
                    settings.shader.setUniform("ProjMat") { set(data.projMat) }
                    settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((-data.camera.pos).toJOML(), Matrix4f())) }
                    settings.shader.setUniform("CameraPos") { set(data.camera.pos) }

                    lights.forEachIndexed { index, light -> light.first.update(manager, VibrancyConfig.entityShadowsEnabled && index < VibrancyConfig.entityShadowMaxLights && manager.inRenderDistance(light.second, VibrancyConfig.entityShadowDistance)) }
                    lights.forEach { light -> light.first.render(settings.shader, debugOut) }
                }
            }
        }
    }
}