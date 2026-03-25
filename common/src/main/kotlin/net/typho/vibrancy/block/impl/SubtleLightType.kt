package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.shaders.NeoShaderRegistry
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.shadows.LightMesh

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLightStorage> {
    @JvmStatic
    fun meshBlitSettings(data: RenderEventData, chunk: SubtleLightStorage.Chunk) = RenderSettings(
        Vibrancy.id("block/subtle/blit"),
        listOf(
            DisableFlagsShard(listOf(
                GlFlag.DEPTH_TEST,
                GlFlag.CULL_FACE,
                GlFlag.BLEND
            )),
            BindBufferBaseShard(
                { chunk.ssbo },
                0
            ),
            FramebufferShard(
                { chunk.mesh.value!!.target },
                true
            ),
            ShaderShard(
                Vibrancy.id("block/subtle/blit")
            ) { shader ->
                shader.setCommonUniforms(data)

                shader.getUniform("LightBrightness")?.setValue(Vibrancy.config.blockLights.subtle.brightness)
            }
        )
    )

    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = SubtleLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): SubtleLightInfo? {
        return info as? SubtleLightInfo
    }

    override fun createStorage(manager: LightManager) = SubtleLightStorage()

    override fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: SubtleLightStorage,
        fbo: GlFramebuffer,
        debugOut: (String, Int) -> Unit
    ) {
        if (Vibrancy.config.blockLights.subtle.enabled) {
            lights.checkDirty(manager, data)

            val settings = LightMesh.renderSettings(fbo, data, TextureUtil.INSTANCE.blockAtlas)
            val shader = NeoShaderRegistry.get(Vibrancy.id("light_mesh"))!! // TODO

            settings.bind()

            lights.chunks.values
                .filter {
                    manager.inRenderDistance(data, it.pos, Vibrancy.config.blockLights.subtle.renderDistance)
                }
                .forEach {
                    it.render(data, shader, debugOut)
                }

            settings.unbind()
        }
    }
}