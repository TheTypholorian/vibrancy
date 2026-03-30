package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.toJOML
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
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
        data: RenderEventData,
        lights: HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>,
        debugOut: (String, Int) -> Unit
    ) {
        if (Vibrancy.config.blockLights.raytraced.enabled) {
            LightMesh.drawState(NeoAtlas.blocks, Vibrancy.id("block/raytraced/mesh")).bind().use { settings ->
                settings.shader.setTexture(2, GlTextureBinding.FromInstance(
                    ReflectionAtlases[NeoIdentifier("blocks")], //NeoAtlas.blocks.location
                    GlTextureTarget.TEXTURE_2D
                ))
                settings.shader.setUniform("CameraPos") { set(data.camera.pos) }

                lights.map.values.forEach { it.update(manager, data) }
                lights.map.values
                    .filter { light ->
                        data.frustum.testAab(
                            light.boundingBox.min.toFloat().toJOML(),
                            light.boundingBox.max.toFloat().toJOML(),
                        )
                    }
                    .sortedBy { light -> manager.getSortingOrder(data, light.pos) }
                    .take(Vibrancy.config.blockLights.raytraced.maxRendered)
                    .forEach { light -> light.render(settings.shader, debugOut) }
            }
        }
    }
}