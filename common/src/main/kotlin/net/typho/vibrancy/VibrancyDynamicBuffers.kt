package net.typho.vibrancy

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
import net.typho.big_shot_lib.spirv.ShaderMixinContext.Companion.WORD_SIZE_BYTES
import net.typho.big_shot_lib.spirv.at.AtOpcode
import net.typho.big_shot_lib.spirv.at.AtVoidReturn
import net.typho.big_shot_lib.spirv.vars.ShaderPrimitiveType
import net.typho.big_shot_lib.spirv.vars.ShaderVectorType
import org.jetbrains.annotations.ApiStatus
import org.lwjgl.opengl.GL11.GL_NONE
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

object VibrancyDynamicBuffers : ShaderMixinCallback {
    private var initialized = false
    @JvmField
    var normalsLocation: Int = 0
    @JvmField
    var normalsTexture: ITexture? = null
    @JvmField
    var albedoLocation: Int = 0
    @JvmField
    var albedoTexture: ITexture? = null
    @JvmField
    var lightUVLocation: Int = 0
    @JvmField
    var lightUVTexture: ITexture? = null

    @ApiStatus.Internal
    @JvmStatic
    fun init(width: Int, height: Int) {
        normalsLocation = pickAvailableAttachment()!!
        normalsTexture = NeoTexture(Vibrancy.id("dynamic_buffer/normals"), GlResourceType.TEXTURE_2D, TextureFormat.RGB16_SNORM)

        /*
        albedoLocation = pickAvailableAttachment()!!
        albedoTexture = NeoTexture(Vibrancy.id("dynamic_buffer/albedo"), GlResourceType.TEXTURE_2D, TextureFormat.RGB)

        lightUVLocation = pickAvailableAttachment()!!
        lightUVTexture = NeoTexture(Vibrancy.id("dynamic_buffer/light_uv"), GlResourceType.TEXTURE_2D, TextureFormat.RG)
         */

        attach(width, height)

        initialized = true
    }

    @ApiStatus.Internal
    @JvmStatic
    fun attach(width: Int, height: Int) {
        normalsTexture!!.bind().use {
            normalsTexture!!.resize2D(width, height)
            normalsTexture!!.attach2D(GL_COLOR_ATTACHMENT0 + normalsLocation, GL_FRAMEBUFFER)
        }
        /*
        albedoTexture!!.bind().use {
            albedoTexture!!.resize2D(width, height)
            albedoTexture!!.attach2D(GL_COLOR_ATTACHMENT0 + albedoLocation, GL_FRAMEBUFFER)
        }
        lightUVTexture!!.bind().use {
            lightUVTexture!!.resize2D(width, height)
            lightUVTexture!!.attach2D(GL_COLOR_ATTACHMENT0 + lightUVLocation, GL_FRAMEBUFFER)
        }
         */
    }

    @ApiStatus.Internal
    @JvmStatic
    fun resize(width: Int, height: Int) {
        if (initialized) {
            attach(width, height)
        } else {
            init(width, height)
        }

        glDrawBuffers(intArrayOf(0, normalsLocation))
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
                        val normalId = context.locateVariable(
                            name = format.getElementName(VertexFormatElement.NORMAL)
                        )

                        if (normalId != null) {
                            vec3 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 3).findOrInject(context)

                            val output = context.addStaticVar(3, vec3, "VibrancyVertexNormal")
                            context.addEntrypointVars(output)

                            val location = locations.getMapper(3, type)!!.map("VibrancyVertexNormal")
                            context.inject(
                                AtOpcode(71), // OpDecorate
                                ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                    .order(ShaderMixinContext.BYTE_ORDER)
                                    .putInt(0x00_04_00_47)
                                    .putInt(output)
                                    .putInt(30) // Location
                                    .putInt(location)
                            )

                            val tempVar = context.bound++
                            context.putBound()
                            context.inject(
                                AtVoidReturn("main"),
                                ByteBuffer.allocate(7 * WORD_SIZE_BYTES)
                                    .order(ShaderMixinContext.BYTE_ORDER)

                                    .putInt(0x00_04_00_3D)
                                    .putInt(vec3)
                                    .putInt(tempVar)
                                    .putInt(normalId)

                                    .putInt(0x00_03_00_3E)
                                    .putInt(output)
                                    .putInt(tempVar)
                            )
                        }
                    }
                }
                ShaderType.FRAGMENT -> {
                    var vec3: Int = -1

                    if (format.contains(VertexFormatElement.NORMAL)) {
                        val mapper = locations.getMapper(1, type)!!
                        val inputLocation = mapper.map.get("VibrancyVertexNormal")

                        if (inputLocation == null) {
                            return
                        }

                        vec3 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 3).inject(context)

                        val input = context.addStaticVar(1, vec3, "VibrancyVertexNormal")

                        context.inject(
                            AtOpcode(71), // OpDecorate
                            ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                .order(ShaderMixinContext.BYTE_ORDER)
                                .putInt(0x00_04_00_47)
                                .putInt(input)
                                .putInt(30) // Location
                                .putInt(inputLocation)
                        )

                        val output = context.addStaticVar(3, vec3, "VibrancyFragmentNormal")

                        context.addEntrypointVars(input, output)

                        context.inject(
                            AtOpcode(71), // OpDecorate
                            ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                .order(ShaderMixinContext.BYTE_ORDER)
                                .putInt(0x00_04_00_47)
                                .putInt(output)
                                .putInt(30) // Location
                                .putInt(normalsLocation)
                        )

                        val tempVar = context.bound++
                        context.putBound()
                        context.inject(
                            AtVoidReturn("main"),
                            ByteBuffer.allocate(7 * WORD_SIZE_BYTES)
                                .order(ShaderMixinContext.BYTE_ORDER)

                                .putInt(0x00_04_00_3D)
                                .putInt(vec3)
                                .putInt(tempVar)
                                .putInt(input)

                                .putInt(0x00_03_00_3E)
                                .putInt(output)
                                .putInt(tempVar)
                        )
                    }
                }
            }
        }
    }
}