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
import net.typho.big_shot_lib.spirv.at.BeforeFirstFunction
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

        albedoLocation = pickAvailableAttachment(normalsLocation)!!
        albedoTexture = NeoTexture(Vibrancy.id("dynamic_buffer/albedo"), GlResourceType.TEXTURE_2D, TextureFormat.RGB)

        lightUVLocation = pickAvailableAttachment(normalsLocation, albedoLocation)!!
        lightUVTexture = NeoTexture(Vibrancy.id("dynamic_buffer/light_uv"), GlResourceType.TEXTURE_2D, TextureFormat.RG)

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
        albedoTexture!!.bind().use {
            albedoTexture!!.resize2D(width, height)
            albedoTexture!!.attach2D(GL_COLOR_ATTACHMENT0 + albedoLocation, GL_FRAMEBUFFER)
        }
        lightUVTexture!!.bind().use {
            lightUVTexture!!.resize2D(width, height)
            lightUVTexture!!.attach2D(GL_COLOR_ATTACHMENT0 + lightUVLocation, GL_FRAMEBUFFER)
        }
    }

    @ApiStatus.Internal
    @JvmStatic
    fun resize(width: Int, height: Int) {
        if (initialized) {
            attach(width, height)
        } else {
            init(width, height)
        }

        initState()
    }

    @ApiStatus.Internal
    @JvmStatic
    fun initState() {
        glDrawBuffers(intArrayOf(
            GL_COLOR_ATTACHMENT0,
            GL_COLOR_ATTACHMENT0 + normalsLocation,
            GL_COLOR_ATTACHMENT0 + albedoLocation,
            GL_COLOR_ATTACHMENT0 + lightUVLocation
        ))
        glDisablei(GL_BLEND, normalsLocation)
        glDisablei(GL_BLEND, albedoLocation)
        glDisablei(GL_BLEND, lightUVLocation)
    }

    @JvmStatic
    fun pickAvailableAttachment(vararg exclude: Int): Int? {
        val max = glGetInteger(GL_MAX_COLOR_ATTACHMENTS)

        repeat(max) { i ->
            if (
                glGetFramebufferAttachmentParameteri(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0 + i,
                    GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
                ) == GL_NONE && !exclude.contains(i)
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
        if (shader == ResourceLocation.withDefaultNamespace("rendertype_lines")) {
            return
        }

        if (locations.hasGeometryShader) {
            if (format != null) {
                if (
                    format.contains(VertexFormatElement.NORMAL) ||
                    format.contains(VertexFormatElement.UV0) ||
                    format.contains(VertexFormatElement.UV2)
                ) {
                    Vibrancy.LOGGER.warn("Vibrancy dynamic buffers cannot apply to shader $shader with format $format because it has a geometry shader, skipping")
                }
            }

            return
        }

        format?.let {
            when (type) {
                ShaderType.VERTEX -> {
                    if (format.contains(VertexFormatElement.NORMAL)) {
                        val normalVar = context.locateVariable(
                            name = format.getElementName(VertexFormatElement.NORMAL)
                        )

                        if (normalVar != null) {
                            val output = context.addStaticVar(3, normalVar.type, "VibrancyVertexNormal")
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

                                    .putInt(0x00_04_00_3D) // OpLoad
                                    .putInt(normalVar.type)
                                    .putInt(tempVar)
                                    .putInt(normalVar.id)

                                    .putInt(0x00_03_00_3E) // OpStore
                                    .putInt(output)
                                    .putInt(tempVar)
                            )
                        }
                    }

                    if (format.contains(VertexFormatElement.UV2)) {
                        val lightVar = context.locateVariable(
                            name = format.getElementName(VertexFormatElement.UV2)
                        )

                        if (lightVar != null) {
                            val floatType = ShaderPrimitiveType.FLOAT_32.findOrInject(context)
                            val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(context)

                            val output = context.addStaticVar(3, vec2, "VibrancyVertexLight")
                            context.addEntrypointVars(output)

                            val location = locations.getMapper(3, type)!!.map("VibrancyVertexLight")
                            context.inject(
                                AtOpcode(71), // OpDecorate
                                ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                    .order(ShaderMixinContext.BYTE_ORDER)
                                    .putInt(0x00_04_00_47)
                                    .putInt(output)
                                    .putInt(30) // Location
                                    .putInt(location)
                            )

                            val constantVar = context.bound++
                            val tempVar = context.bound++
                            val tempVar2 = context.bound++
                            context.putBound()

                            context.inject(
                                BeforeFirstFunction,
                                ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                    .order(ShaderMixinContext.BYTE_ORDER)

                                    .putInt(0x00_04_00_2B) // OpConstant
                                    .putInt(floatType)
                                    .putInt(constantVar)
                                    .putFloat(256f)
                            )

                            context.inject(
                                AtVoidReturn("main"),
                                ByteBuffer.allocate(12 * WORD_SIZE_BYTES)
                                    .order(ShaderMixinContext.BYTE_ORDER)

                                    .putInt(0x00_04_00_3D) // OpLoad
                                    .putInt(vec2)
                                    .putInt(tempVar)
                                    .putInt(lightVar.id)

                                    .putInt(0x00_05_00_88) // OpFDiv
                                    .putInt(vec2)
                                    .putInt(tempVar2)
                                    .putInt(tempVar)
                                    .putInt(constantVar)

                                    .putInt(0x00_03_00_3E) // OpStore
                                    .putInt(output)
                                    .putInt(tempVar2)
                            )
                        }
                    }

                    if (format.contains(VertexFormatElement.UV0)) {
                        val uvVar = context.locateVariable(
                            name = format.getElementName(VertexFormatElement.UV0)
                        )

                        if (uvVar != null) {
                            val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(context)

                            val output = context.addStaticVar(3, vec2, "VibrancyVertexTexCoord")
                            context.addEntrypointVars(output)

                            val location = locations.getMapper(3, type)!!.map("VibrancyVertexTexCoord")
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

                                    .putInt(0x00_04_00_3D) // OpLoad
                                    .putInt(vec2)
                                    .putInt(tempVar)
                                    .putInt(uvVar.id)

                                    .putInt(0x00_03_00_3E) // OpStore
                                    .putInt(output)
                                    .putInt(tempVar)
                            )
                        }
                    }
                }
                ShaderType.GEOMETRY -> throw AssertionError()
                ShaderType.FRAGMENT -> {
                    if (format.contains(VertexFormatElement.NORMAL)) {
                        val mapper = locations.getMapper(1, type)!!
                        val inputLocation = mapper.map.get("VibrancyVertexNormal")

                        if (inputLocation != null) {
                            val vec3 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 3).findOrInject(context)

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

                                    .putInt(0x00_04_00_3D) // OpLoad
                                    .putInt(vec3)
                                    .putInt(tempVar)
                                    .putInt(input)

                                    .putInt(0x00_03_00_3E) // OpStore
                                    .putInt(output)
                                    .putInt(tempVar)
                            )
                        }
                    }

                    if (format.contains(VertexFormatElement.UV2)) {
                        val mapper = locations.getMapper(1, type)!!
                        val inputLocation = mapper.map.get("VibrancyVertexLight")

                        if (inputLocation != null) {
                            val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(context)

                            val input = context.addStaticVar(1, vec2, "VibrancyVertexLight")

                            context.inject(
                                AtOpcode(71), // OpDecorate
                                ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                    .order(ShaderMixinContext.BYTE_ORDER)
                                    .putInt(0x00_04_00_47)
                                    .putInt(input)
                                    .putInt(30) // Location
                                    .putInt(inputLocation)
                            )

                            val output = context.addStaticVar(3, vec2, "VibrancyFragmentLight")

                            context.addEntrypointVars(input, output)

                            context.inject(
                                AtOpcode(71), // OpDecorate
                                ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                    .order(ShaderMixinContext.BYTE_ORDER)
                                    .putInt(0x00_04_00_47)
                                    .putInt(output)
                                    .putInt(30) // Location
                                    .putInt(lightUVLocation)
                            )

                            val tempVar = context.bound++
                            context.putBound()
                            context.inject(
                                AtVoidReturn("main"),
                                ByteBuffer.allocate(7 * WORD_SIZE_BYTES)
                                    .order(ShaderMixinContext.BYTE_ORDER)

                                    .putInt(0x00_04_00_3D) // OpLoad
                                    .putInt(vec2)
                                    .putInt(tempVar)
                                    .putInt(input)

                                    .putInt(0x00_03_00_3E) // OpStore
                                    .putInt(output)
                                    .putInt(tempVar)
                            )
                        }
                    }

                    if (format.contains(VertexFormatElement.UV0)) {
                        val mapper = locations.getMapper(1, type)!!
                        val inputLocation = mapper.map.get("VibrancyVertexTexCoord")

                        if (inputLocation != null) {
                            val sampler0Var = context.locateVariable(
                                name = "Sampler0"
                            )

                            if (sampler0Var != null) {
                                val vec4 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 4).findOrInject(context)
                                val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(context)

                                val input = context.addStaticVar(1, vec2, "VibrancyVertexTexCoord")

                                context.inject(
                                    AtOpcode(71), // OpDecorate
                                    ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                        .order(ShaderMixinContext.BYTE_ORDER)
                                        .putInt(0x00_04_00_47)
                                        .putInt(input)
                                        .putInt(30) // Location
                                        .putInt(inputLocation)
                                )

                                val output = context.addStaticVar(3, vec4, "VibrancyFragmentAlbedo")

                                context.addEntrypointVars(input, output)

                                context.inject(
                                    AtOpcode(71), // OpDecorate
                                    ByteBuffer.allocate(4 * WORD_SIZE_BYTES)
                                        .order(ShaderMixinContext.BYTE_ORDER)
                                        .putInt(0x00_04_00_47)
                                        .putInt(output)
                                        .putInt(30) // Location
                                        .putInt(albedoLocation)
                                )

                                val tempSamplerVar = context.bound++
                                val tempTexCoordVar = context.bound++
                                val tempResultVar = context.bound++
                                context.putBound()
                                context.inject(
                                    AtVoidReturn("main"),
                                    ByteBuffer.allocate(16 * WORD_SIZE_BYTES)
                                        .order(ShaderMixinContext.BYTE_ORDER)

                                        .putInt(0x00_04_00_3D) // OpLoad
                                        .putInt(sampler0Var.type)
                                        .putInt(tempSamplerVar)
                                        .putInt(sampler0Var.id)

                                        .putInt(0x00_04_00_3D) // OpLoad
                                        .putInt(vec4)
                                        .putInt(tempTexCoordVar)
                                        .putInt(input)

                                        .putInt(0x00_05_00_57) // OpImageSampleImplicitLod
                                        .putInt(vec4)
                                        .putInt(tempResultVar)
                                        .putInt(tempSamplerVar)
                                        .putInt(tempTexCoordVar)

                                        .putInt(0x00_03_00_3E) // OpStore
                                        .putInt(output)
                                        .putInt(tempResultVar)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}