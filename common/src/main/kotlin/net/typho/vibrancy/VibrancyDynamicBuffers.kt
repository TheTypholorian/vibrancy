package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoTexture
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.big_shot_lib.gl.resource.ShaderType
import net.typho.big_shot_lib.gl.resource.TextureFormat
import net.typho.big_shot_lib.spirv.Opcode
import net.typho.big_shot_lib.spirv.ShaderLocationsInfo
import net.typho.big_shot_lib.spirv.ShaderMixinCallback
import net.typho.big_shot_lib.spirv.ShaderMixinContext
import net.typho.big_shot_lib.spirv.vars.ShaderPrimitiveType
import net.typho.big_shot_lib.spirv.vars.ShaderVariable
import net.typho.big_shot_lib.spirv.vars.ShaderVectorType
import org.jetbrains.annotations.ApiStatus
import org.lwjgl.opengl.GL11.GL_NONE
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL30.*

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

    init {
        RenderSystem.recordRenderCall {
            if (glGetInteger(GL_MAX_DRAW_BUFFERS) < 4) {
                throw UnsupportedOperationException("Vibrancy needs GL_MAX_DRAW_BUFFERS to be at least 4 to run")
            }
        }
    }

    @ApiStatus.Internal
    @JvmStatic
    fun init(width: Int, height: Int) {
        normalsLocation = pickAvailableAttachment()!!
        normalsTexture =
            NeoTexture(Vibrancy.id("dynamic_buffer/normals"), GlResourceType.TEXTURE_2D, TextureFormat.RGB16_SNORM)

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
        normalsTexture!!.bind()
        normalsTexture!!.resize2D(width, height)
        normalsTexture!!.attach2D(GL_COLOR_ATTACHMENT0 + normalsLocation, GL_FRAMEBUFFER)
        normalsTexture!!.unbind()

        albedoTexture!!.bind()
        albedoTexture!!.resize2D(width, height)
        albedoTexture!!.attach2D(GL_COLOR_ATTACHMENT0 + albedoLocation, GL_FRAMEBUFFER)
        albedoTexture!!.unbind()

        lightUVTexture!!.bind()
        lightUVTexture!!.resize2D(width, height)
        lightUVTexture!!.attach2D(GL_COLOR_ATTACHMENT0 + lightUVLocation, GL_FRAMEBUFFER)
        lightUVTexture!!.unbind()
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
        glDrawBuffers(
            intArrayOf(
                GL_COLOR_ATTACHMENT0,
                GL_COLOR_ATTACHMENT0 + normalsLocation,
                GL_COLOR_ATTACHMENT0 + albedoLocation,
                GL_COLOR_ATTACHMENT0 + lightUVLocation
            )
        )
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

    override fun mixinPostCompile(
        shader: ResourceLocation,
        type: ShaderType,
        format: VertexFormat?,
        context: ShaderMixinContext,
        locations: ShaderLocationsInfo
    ) {
        if (shader == ResourceLocation.withDefaultNamespace("rendertype_lines")) {
            return
        }

        if (shader == ResourceLocation.withDefaultNamespace("rendertype_end_portal")) {
            if (type == ShaderType.FRAGMENT) {
            }

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
                    if (format.contains(VertexFormatElement.NORMAL) || shader.namespace == "sodium") {
                        var normalVar: ShaderVariable? = null

                        if (format.contains(VertexFormatElement.NORMAL)) {
                            normalVar = context.locateVariable(
                                name = format.getElementName(VertexFormatElement.NORMAL)
                            )
                        }

                        if (normalVar == null && shader.namespace == "sodium") {
                            normalVar = context.locateVariable(
                                name = "a_Normal"
                            )

                            if (normalVar == null) {
                                normalVar = context.locateVariable(
                                    name = "VeilNormal"
                                )

                                if (normalVar == null) {
                                    val vec4 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 4).findOrInject(context)
                                    val input = context.addStaticVar(1, vec4, "VibrancyInputNormal")
                                    context.addEntrypointVars(input.id)

                                    val location = locations.getMapper(1, type)!!.map("VibrancyInputNormal")
                                    context.inject(
                                        context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                        Opcode.Builder(Opcode.OP_DECORATE)
                                            .putInt(input.id)
                                            .putInt(30) // Location
                                            .putInt(location)
                                            .build()
                                    )

                                    normalVar = ShaderVariable(
                                        input.id,
                                        input.name,
                                        location,
                                        input.typePointer,
                                        input.type
                                    )
                                }
                            }
                        }

                        if (normalVar != null) {
                            val output = context.addStaticVar(3, normalVar.type, "VibrancyVertexNormal")
                            context.addEntrypointVars(output.id)

                            val location = locations.getMapper(3, type)!!.map("VibrancyVertexNormal")
                            context.inject(
                                context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                Opcode.Builder(Opcode.OP_DECORATE)
                                    .putInt(output.id)
                                    .putInt(30) // Location
                                    .putInt(location)
                                    .build()
                            )

                            val tempVar = context.bound++
                            context.putBound()
                            context.inject(
                                context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                Opcode.Builder(Opcode.OP_LOAD)
                                    .putInt(normalVar.type)
                                    .putInt(tempVar)
                                    .putInt(normalVar.id)
                                    .build(),
                                Opcode.Builder(Opcode.OP_STORE)
                                    .putInt(output.id)
                                    .putInt(tempVar)
                                    .build()
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
                            context.addEntrypointVars(output.id)

                            val location = locations.getMapper(3, type)!!.map("VibrancyVertexLight")
                            context.inject(
                                context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                Opcode.Builder(Opcode.OP_DECORATE)
                                    .putInt(output.id)
                                    .putInt(30) // Location
                                    .putInt(location)
                                    .build()
                            )

                            val constantVar = context.bound++
                            val tempVar = context.bound++
                            val tempVar2 = context.bound++
                            context.putBound()

                            context.inject(
                                context.locateOpcode(Opcode.OP_FUNCTION)!!.index,
                                Opcode.Builder(Opcode.OP_CONSTANT)
                                    .putInt(floatType)
                                    .putInt(constantVar)
                                    .putFloat(256f)
                                    .build()
                            )

                            context.inject(
                                context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                Opcode.Builder(Opcode.OP_LOAD)
                                    .putInt(vec2)
                                    .putInt(tempVar)
                                    .putInt(lightVar.id)
                                    .build(),
                                Opcode.Builder(Opcode.OP_F_DIV)
                                    .putInt(vec2)
                                    .putInt(tempVar2)
                                    .putInt(tempVar)
                                    .putInt(constantVar)
                                    .build(),
                                Opcode.Builder(Opcode.OP_STORE)
                                    .putInt(output.id)
                                    .putInt(tempVar2)
                                    .build()
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
                            context.addEntrypointVars(output.id)

                            val location = locations.getMapper(3, type)!!.map("VibrancyVertexTexCoord")
                            context.inject(
                                context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                Opcode.Builder(Opcode.OP_DECORATE)
                                    .putInt(output.id)
                                    .putInt(30) // Location
                                    .putInt(location)
                                    .build()
                            )

                            val tempVar = context.bound++
                            context.putBound()
                            context.inject(
                                context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                Opcode.Builder(Opcode.OP_LOAD)
                                    .putInt(vec2)
                                    .putInt(tempVar)
                                    .putInt(uvVar.id)
                                    .build(),
                                Opcode.Builder(Opcode.OP_STORE)
                                    .putInt(output.id)
                                    .putInt(tempVar)
                                    .build()
                            )

                            if (format.contains(VertexFormatElement.COLOR)) {
                                val colorVar = context.locateVariable(
                                    name = format.getElementName(VertexFormatElement.COLOR)
                                )

                                if (colorVar != null) {
                                    val vec4 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 4).findOrInject(context)

                                    val colorOutput = context.addStaticVar(3, vec4, "VibrancyVertexColor")
                                    context.addEntrypointVars(colorOutput.id)

                                    val colorLocation = locations.getMapper(3, type)!!.map("VibrancyVertexColor")
                                    context.inject(
                                        context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                        Opcode.Builder(Opcode.OP_DECORATE)
                                            .putInt(colorOutput.id)
                                            .putInt(30) // Location
                                            .putInt(colorLocation)
                                            .build()
                                    )

                                    val tempColorVar = context.bound++
                                    context.putBound()
                                    context.inject(
                                        context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                        Opcode.Builder(Opcode.OP_LOAD)
                                            .putInt(vec4)
                                            .putInt(tempColorVar)
                                            .putInt(colorVar.id)
                                            .build(),
                                        Opcode.Builder(Opcode.OP_STORE)
                                            .putInt(colorOutput.id)
                                            .putInt(tempColorVar)
                                            .build()
                                    )
                                }
                            }
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
                                context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                Opcode.Builder(Opcode.OP_DECORATE)
                                    .putInt(input.id)
                                    .putInt(30) // Location
                                    .putInt(inputLocation)
                                    .build()
                            )

                            val output = context.addStaticVar(3, vec3, "VibrancyFragmentNormal")

                            context.addEntrypointVars(input.id, output.id)

                            context.inject(
                                context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                Opcode.Builder(Opcode.OP_DECORATE)
                                    .putInt(output.id)
                                    .putInt(30) // Location
                                    .putInt(normalsLocation)
                                    .build()
                            )

                            val tempVar = context.bound++
                            context.putBound()
                            context.inject(
                                context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                Opcode.Builder(Opcode.OP_LOAD)
                                    .putInt(vec3)
                                    .putInt(tempVar)
                                    .putInt(input.id)
                                    .build(),
                                Opcode.Builder(Opcode.OP_STORE)
                                    .putInt(output.id)
                                    .putInt(tempVar)
                                    .build()
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
                                context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                Opcode.Builder(Opcode.OP_DECORATE)
                                    .putInt(input.id)
                                    .putInt(30) // Location
                                    .putInt(inputLocation)
                                    .build()
                            )

                            val output = context.addStaticVar(3, vec2, "VibrancyFragmentLight")

                            context.addEntrypointVars(input.id, output.id)

                            context.inject(
                                context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                Opcode.Builder(Opcode.OP_DECORATE)
                                    .putInt(output.id)
                                    .putInt(30) // Location
                                    .putInt(lightUVLocation)
                                    .build()
                            )

                            val tempVar = context.bound++
                            context.putBound()
                            context.inject(
                                context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                Opcode.Builder(Opcode.OP_LOAD)
                                    .putInt(vec2)
                                    .putInt(tempVar)
                                    .putInt(input.id)
                                    .build(),
                                Opcode.Builder(Opcode.OP_STORE)
                                    .putInt(output.id)
                                    .putInt(tempVar)
                                    .build()
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
                                    context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                    Opcode.Builder(Opcode.OP_DECORATE)
                                        .putInt(input.id)
                                        .putInt(30) // Location
                                        .putInt(inputLocation)
                                        .build()
                                )

                                val output = context.addStaticVar(3, vec4, "VibrancyFragmentAlbedo")

                                context.addEntrypointVars(input.id, output.id)

                                context.inject(
                                    context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                    Opcode.Builder(Opcode.OP_DECORATE)
                                        .putInt(output.id)
                                        .putInt(30) // Location
                                        .putInt(albedoLocation)
                                        .build()
                                )

                                val vertexColorLocation = mapper.map.get("VibrancyVertexColor")

                                if (vertexColorLocation != null) {
                                    val vertexColor = context.addStaticVar(1, vec4, "VibrancyVertexColor")

                                    context.inject(
                                        context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                        Opcode.Builder(Opcode.OP_DECORATE)
                                            .putInt(vertexColor.id)
                                            .putInt(30) // Location
                                            .putInt(vertexColorLocation)
                                            .build()
                                    )

                                    val tempSamplerVar = context.bound++
                                    val tempTexCoordVar = context.bound++
                                    val tempColorVar = context.bound++
                                    val tempPreResultVar = context.bound++
                                    val tempResultVar = context.bound++
                                    context.putBound()
                                    context.inject(
                                        context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                        Opcode.Builder(Opcode.OP_LOAD)
                                            .putInt(sampler0Var.type)
                                            .putInt(tempSamplerVar)
                                            .putInt(sampler0Var.id)
                                            .build(),
                                        Opcode.Builder(Opcode.OP_LOAD)
                                            .putInt(vertexColor.type)
                                            .putInt(tempColorVar)
                                            .putInt(vertexColor.id)
                                            .build(),
                                        Opcode.Builder(Opcode.OP_LOAD)
                                            .putInt(vec4)
                                            .putInt(tempTexCoordVar)
                                            .putInt(input.id)
                                            .build(),

                                        Opcode.Builder(Opcode.OP_IMAGE_SAMPLE_IMPLICIT_LOD)
                                            .putInt(vec4)
                                            .putInt(tempPreResultVar)
                                            .putInt(tempSamplerVar)
                                            .putInt(tempTexCoordVar)
                                            .build(),
                                        Opcode.Builder(Opcode.OP_F_MUL)
                                            .putInt(vec4)
                                            .putInt(tempResultVar)
                                            .putInt(tempPreResultVar)
                                            .putInt(tempColorVar)
                                            .build(),

                                        Opcode.Builder(Opcode.OP_STORE)
                                            .putInt(output.id)
                                            .putInt(tempResultVar)
                                            .build()
                                    )
                                } else {
                                    val tempSamplerVar = context.bound++
                                    val tempTexCoordVar = context.bound++
                                    val tempResultVar = context.bound++
                                    context.putBound()
                                    context.inject(
                                        context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                        Opcode.Builder(Opcode.OP_LOAD)
                                            .putInt(sampler0Var.type)
                                            .putInt(tempSamplerVar)
                                            .putInt(sampler0Var.id)
                                            .build(),
                                        Opcode.Builder(Opcode.OP_LOAD)
                                            .putInt(vec4)
                                            .putInt(tempTexCoordVar)
                                            .putInt(input.id)
                                            .build(),

                                        Opcode.Builder(Opcode.OP_IMAGE_SAMPLE_IMPLICIT_LOD)
                                            .putInt(vec4)
                                            .putInt(tempResultVar)
                                            .putInt(tempSamplerVar)
                                            .putInt(tempTexCoordVar)
                                            .build(),

                                        Opcode.Builder(Opcode.OP_STORE)
                                            .putInt(output.id)
                                            .putInt(tempResultVar)
                                            .build()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}