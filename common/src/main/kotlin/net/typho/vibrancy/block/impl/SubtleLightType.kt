package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferType
import net.typho.big_shot_lib.api.client.opengl.buffers.ClearBit
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType

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
                { chunk.mesh.mesh.vbo.cast(BufferType.SHADER_STORAGE_BUFFER) },
                0
            ),
            BindBufferBaseShard(
                { chunk.mesh.atlas },
                1
            ),
            BindBufferBaseShard(
                { chunk.ssbo },
                2
            ),
            FramebufferShard(
                { chunk.mesh.target },
                true,
                ClearBit.Color(IColor.FULL_OFF)
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

    override fun render(manager: LightManager, data: RenderEventData, lights: SubtleLightStorage, fbo: GlFramebuffer): LightRenderResult {
        val result = LightRenderResult(numRendered = 0)

        if (Vibrancy.config.blockLights.subtle.enabled) {
            lights.checkDirty(manager, data)

            val meshes = lights.chunks.values.sortedBy { manager.getSortingOrder(data, it.pos) }

            for (mesh in meshes) {
                if ((result.numRendered ?: 0) + mesh.size > Vibrancy.config.blockLights.subtle.maxRendered) {
                    break
                }

                result.add(mesh.render(fbo, data))
            }
        }

        return result
    }
}