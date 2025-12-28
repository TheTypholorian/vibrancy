package net.typho.vibrancy

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoTexture
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.big_shot_lib.gl.resource.ShaderType
import net.typho.big_shot_lib.gl.resource.TextureFormat
import net.typho.big_shot_lib.spirv.ShaderLocationsInfo
import net.typho.big_shot_lib.spirv.ShaderMixinCallback
import net.typho.big_shot_lib.spirv.ShaderMixinContext
import net.typho.big_shot_lib.spirv.vars.ShaderPrimitiveType
import net.typho.big_shot_lib.spirv.vars.ShaderVectorType
import org.jetbrains.annotations.ApiStatus
import org.lwjgl.opengl.GL11.GL_NONE
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL30.*

object VibrancyDynamicBuffers : ShaderMixinCallback {
    @JvmField
    var normals: Int = 0
    @JvmField
    var normalsTexture: ITexture? = null
    @JvmField
    var albedo: Int = 0
    @JvmField
    var albedoTexture: ITexture? = null
    @JvmField
    var lightUV: Int = 0
    @JvmField
    var lightUVTexture: ITexture? = null

    @ApiStatus.Internal
    @JvmStatic
    fun init(fbo: RenderTarget) {
        fbo.bindWrite(false)

        normals = pickAvailableAttachment()!!
        normalsTexture = NeoTexture(Vibrancy.id("dynamic_buffer/normals"), GlResourceType.TEXTURE_2D, TextureFormat.RGB16_SNORM)
        normalsTexture!!.bind().use {
            normalsTexture!!.attach(GL_COLOR_ATTACHMENT0 + normals, GL_FRAMEBUFFER)
        }

        albedo = pickAvailableAttachment()!!
        albedoTexture = NeoTexture(Vibrancy.id("dynamic_buffer/albedo"), GlResourceType.TEXTURE_2D, TextureFormat.RGB)
        albedoTexture!!.bind().use {
            albedoTexture!!.attach(GL_COLOR_ATTACHMENT0 + albedo, GL_FRAMEBUFFER)
        }

        lightUV = pickAvailableAttachment()!!
        lightUVTexture = NeoTexture(Vibrancy.id("dynamic_buffer/normals"), GlResourceType.TEXTURE_2D, TextureFormat.RG)
        lightUVTexture!!.bind().use {
            lightUVTexture!!.attach(GL_COLOR_ATTACHMENT0 + lightUV, GL_FRAMEBUFFER)
        }

        fbo.unbindWrite()
    }

    @JvmStatic
    fun pickAvailableAttachment(): Int? {
        val max = glGetInteger(GL_MAX_COLOR_ATTACHMENTS)

        repeat(max) { i ->
            if (
                glGetFramebufferAttachmentParameteri(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0 + i,
                    GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
                ) == GL_NONE
            ) {
                return i
            }
        }

        return null
    }

    override fun mixinSpirV(
        shader: ResourceLocation,
        type: ShaderType,
        format: VertexFormat?,
        context: ShaderMixinContext,
        locations: ShaderLocationsInfo
    ) {
        format?.let {
            when (type) {
                ShaderType.VERTEX -> {
                    var vec3: Int = -1

                    if (format.contains(VertexFormatElement.NORMAL)) {
                        vec3 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 3).inject(context)

                        val output = context.addStaticVar(3, vec3, "VibrancyNormalInput")
                        context.addEntrypointVars(output)
                    }
                }
                ShaderType.FRAGMENT -> {
                }
            }
        }
    }
}