package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.shaders.ShaderLoaderType
import net.typho.big_shot_lib.api.shaders.ShaderProgramKey
import net.typho.big_shot_lib.api.shaders.ShaderSourceKey
import net.typho.big_shot_lib.api.shaders.ShaderSourceType
import net.typho.big_shot_lib.api.shaders.mixins.*
import net.typho.big_shot_lib.api.shaders.variables.ShaderPrimitiveType
import net.typho.big_shot_lib.api.shaders.variables.ShaderVariable
import net.typho.big_shot_lib.api.shaders.variables.ShaderVectorType
import net.typho.big_shot_lib.api.textures.GlTexture
import net.typho.big_shot_lib.api.textures.TextureFormat
import org.jetbrains.annotations.ApiStatus
import org.lwjgl.opengl.GL11.GL_NONE
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL30.*

object VibrancyDynamicBuffers : ShaderMixin.Factory {
    private var initialized = false

    @JvmField
    var normalsLocation: Int = 0

    @JvmField
    var normalsTexture: GlTexture? = null

    @JvmField
    var albedoLocation: Int = 0

    @JvmField
    var albedoTexture: GlTexture? = null

    @JvmField
    var lightUVLocation: Int = 0

    @JvmField
    var lightUVTexture: GlTexture? = null

    @JvmField
    val noVertexColor = HashSet<ResourceLocation>(
        RenderType.chunkBufferLayers()
            .filterIsInstance<RenderType.CompositeRenderType>()
            .mapNotNull { type ->
                type.state().shaderState.shader.map { shader ->
                    shader.get()?.name
                }.orElse(null)
            }
            .map { name -> ResourceLocation.withDefaultNamespace(name) }
    )
    @JvmField
    val exclude = HashSet<ResourceLocation>(listOf(
        ResourceLocation.withDefaultNamespace("rendertype_lines"),
        ResourceLocation.withDefaultNamespace("particle")
    ))
    @JvmField
    val builtin = HashSet<ResourceLocation>(listOf(
        ResourceLocation.withDefaultNamespace("rendertype_end_portal")
    ))

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
        normalsTexture = GlTexture(TextureFormat.RGB16_SNORM)

        albedoLocation = pickAvailableAttachment(normalsLocation)!!
        albedoTexture = GlTexture(TextureFormat.RGB)

        lightUVLocation = pickAvailableAttachment(normalsLocation, albedoLocation)!!
        lightUVTexture = GlTexture(TextureFormat.RG)

        attach(width, height)

        initialized = true
    }

    @ApiStatus.Internal
    @JvmStatic
    fun attach(width: Int, height: Int) {
        normalsTexture!!.bind()
        normalsTexture!!.resize(width, height).uploadNull()
        normalsTexture!!.attachToFramebuffer(GL_COLOR_ATTACHMENT0 + normalsLocation)
        normalsTexture!!.unbind()

        albedoTexture!!.bind()
        albedoTexture!!.resize(width, height).uploadNull()
        albedoTexture!!.attachToFramebuffer(GL_COLOR_ATTACHMENT0 + albedoLocation)
        albedoTexture!!.unbind()

        lightUVTexture!!.bind()
        lightUVTexture!!.resize(width, height).uploadNull()
        lightUVTexture!!.attachToFramebuffer(GL_COLOR_ATTACHMENT0 + lightUVLocation)
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
        enableBlend()
    }

    @ApiStatus.Internal
    @JvmStatic
    fun enableBlend() {
        glDisablei(GL_BLEND, normalsLocation)
        glEnablei(GL_BLEND, albedoLocation)
        glDisablei(GL_BLEND, lightUVLocation)
    }

    @ApiStatus.Internal
    @JvmStatic
    fun disableBlend() {
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

    override fun create(key: ShaderProgramKey, parent: ShaderMixinManager.Instance): ShaderMixin {
        val locations = (parent.getOrCreateMixinInstance(ShaderLocationMapperMixin) as ShaderLocationMapperMixin.Instance).locations

        return object : ShaderMixin {
            override fun mixinBytecode(key: ShaderSourceKey, code: ShaderBytecodeBuffer): ShaderBytecodeBuffer {
                if (exclude.contains(key.program.location) || key.program.location.namespace == Vibrancy.MOD_ID) {
                    return code
                }

                if (builtin.contains(key.program.location)) {
                    if (key.type == ShaderSourceType.FRAGMENT) {
                        code.findVariable(name = "VibrancyFragmentNormal")?.let { normal ->
                            code.findOpcode(ShaderOpcode.OP_DECORATE, 0 to normal.id, 1 to 30)
                                ?.putWord(2, normalsLocation)
                        }
                        code.findVariable(name = "VibrancyFragmentLight")?.let { light ->
                            code.findOpcode(ShaderOpcode.OP_DECORATE, 0 to light.id, 1 to 30)
                                ?.putWord(2, lightUVLocation)
                        }
                        code.findVariable(name = "VibrancyFragmentAlbedo")?.let { albedo ->
                            code.findOpcode(ShaderOpcode.OP_DECORATE, 0 to albedo.id, 1 to 30)
                                ?.putWord(2, albedoLocation)
                        }
                    }

                    return code
                }

                if (key.program.sources.contains(ShaderSourceType.GEOMETRY)) {
                    if (
                        key.program.format.contains(VertexFormatElement.NORMAL) ||
                        key.program.format.contains(VertexFormatElement.UV0) ||
                        key.program.format.contains(VertexFormatElement.UV2)
                    ) {
                        Vibrancy.LOGGER.warn("Vibrancy dynamic buffers cannot apply to shader $key because it has a geometry shader, skipping.")
                    }

                    return code
                }

                when (key.type) {
                    ShaderSourceType.VERTEX -> {
                        if (key.program.format.contains(VertexFormatElement.NORMAL)) {
                            val normalVar = code.findVariable(
                                name = key.program.format.getElementName(VertexFormatElement.NORMAL)
                            )

                            if (normalVar != null) {
                                val output = code.addStaticVar(3, normalVar.type, "VibrancyVertexNormal")
                                code.addEntrypointVars(output.id)

                                val location = locations.getMapper(ShaderStorageClass.OUTPUT, key.type)!!.map(1, "VibrancyVertexNormal")
                                code.insert(
                                    code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                    ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                        .putWord(output.id)
                                        .putWord(30) // Location
                                        .putWord(location)
                                        .build()
                                )

                                val tempVar = code.bound++
                                code.insert(
                                    code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                    ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                        .putWord(normalVar.type)
                                        .putWord(tempVar)
                                        .putWord(normalVar.id)
                                        .build(),
                                    ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                        .putWord(output.id)
                                        .putWord(tempVar)
                                        .build()
                                )
                            }
                        }

                        if (key.program.format.contains(VertexFormatElement.UV2)) {
                            val lightVar = code.findVariable(
                                name = key.program.format.getElementName(VertexFormatElement.UV2)
                            ) ?: code.findVariable(
                                name = "_vert_tex_light_coord"
                            )

                            if (lightVar != null) {
                                val floatType = ShaderPrimitiveType.FLOAT_32.findOrInject(code)
                                val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(code)

                                val output = code.addStaticVar(3, vec2, "VibrancyVertexLight")
                                code.addEntrypointVars(output.id)

                                val location = locations.getMapper(ShaderStorageClass.OUTPUT, key.type)!!.map(1, "VibrancyVertexLight")
                                code.insert(
                                    code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                    ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                        .putWord(output.id)
                                        .putWord(30) // Location
                                        .putWord(location)
                                        .build()
                                )

                                val tempVar = code.bound++

                                if (key.program.loader == ShaderLoaderType.SODIUM) {
                                    code.insert(
                                        code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                        ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                            .putWord(vec2)
                                            .putWord(tempVar)
                                            .putWord(lightVar.id)
                                            .build(),
                                        ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                            .putWord(output.id)
                                            .putWord(tempVar)
                                            .build()
                                    )
                                } else {
                                    val constantVar = code.bound++
                                    val tempVar2 = code.bound++

                                    code.insert(
                                        code.findOpcode(ShaderOpcode.OP_FUNCTION)!!.index,
                                        ShaderOpcode.Builder(ShaderOpcode.OP_CONSTANT)
                                            .putWord(floatType)
                                            .putWord(constantVar)
                                            .putFloat(256f)
                                            .build()
                                    )

                                    code.insert(
                                        code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                        ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                            .putWord(vec2)
                                            .putWord(tempVar)
                                            .putWord(lightVar.id)
                                            .build(),
                                        ShaderOpcode.Builder(ShaderOpcode.OP_F_DIV)
                                            .putWord(vec2)
                                            .putWord(tempVar2)
                                            .putWord(tempVar)
                                            .putWord(constantVar)
                                            .build(),
                                        ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                            .putWord(output.id)
                                            .putWord(tempVar2)
                                            .build()
                                    )
                                }
                            }
                        }

                        if (key.program.format.contains(VertexFormatElement.UV0)) {
                            val uvVar: ShaderVariable? = code.findVariable(
                                name = key.program.format.getElementName(VertexFormatElement.UV0)
                            )

                            if (uvVar != null) {
                                val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(code)

                                val output = code.addStaticVar(3, vec2, "VibrancyVertexTexCoord")
                                code.addEntrypointVars(output.id)

                                val location = locations.getMapper(ShaderStorageClass.OUTPUT, key.type)!!.map(1, "VibrancyVertexTexCoord")
                                code.insert(
                                    code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                    ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                        .putWord(output.id)
                                        .putWord(30) // Location
                                        .putWord(location)
                                        .build()
                                )

                                val tempVar = code.bound++
                                code.insert(
                                    code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                    ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                        .putWord(vec2)
                                        .putWord(tempVar)
                                        .putWord(uvVar.id)
                                        .build(),
                                    ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                        .putWord(output.id)
                                        .putWord(tempVar)
                                        .build()
                                )

                                if (key.program.format.contains(VertexFormatElement.COLOR) && !noVertexColor.contains(key.program.location)) {
                                    val colorVar = code.findVariable(
                                        name = key.program.format.getElementName(VertexFormatElement.COLOR)
                                    )

                                    if (colorVar != null) {
                                        val vec4 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 4).findOrInject(code)

                                        val colorOutput = code.addStaticVar(3, vec4, "VibrancyVertexColor")
                                        code.addEntrypointVars(colorOutput.id)

                                        val colorLocation = locations.getMapper(ShaderStorageClass.OUTPUT, key.type)!!.map(1, "VibrancyVertexColor")
                                        code.insert(
                                            code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                            ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                                .putWord(colorOutput.id)
                                                .putWord(30) // Location
                                                .putWord(colorLocation)
                                                .build()
                                        )

                                        val tempColorVar = code.bound++
                                        code.insert(
                                            code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                            ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                                .putWord(vec4)
                                                .putWord(tempColorVar)
                                                .putWord(colorVar.id)
                                                .build(),
                                            ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                                .putWord(colorOutput.id)
                                                .putWord(tempColorVar)
                                                .build()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ShaderSourceType.FRAGMENT -> {
                        val mapper = locations.getMapper(ShaderStorageClass.INPUT, key.type)!!
                        val normalLocation = mapper.get("VibrancyVertexNormal")

                        if (normalLocation != null) {
                            val vec3 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 3).findOrInject(code)

                            val input = code.addStaticVar(1, vec3, "VibrancyVertexNormal")

                            code.insert(
                                code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                    .putWord(input.id)
                                    .putWord(30) // Location
                                    .putWord(normalLocation)
                                    .build()
                            )

                            val output = code.addStaticVar(3, vec3, "VibrancyFragmentNormal")

                            code.addEntrypointVars(input.id, output.id)

                            code.insert(
                                code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                    .putWord(output.id)
                                    .putWord(30) // Location
                                    .putWord(normalsLocation)
                                    .build()
                            )

                            val tempVar = code.bound++
                            code.insert(
                                code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                    .putWord(vec3)
                                    .putWord(tempVar)
                                    .putWord(input.id)
                                    .build(),
                                ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                    .putWord(output.id)
                                    .putWord(tempVar)
                                    .build()
                            )
                        }

                        val lightLocation = mapper.get("VibrancyVertexLight")

                        if (lightLocation != null) {
                            val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(code)

                            val input = code.addStaticVar(1, vec2, "VibrancyVertexLight")

                            code.insert(
                                code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                    .putWord(input.id)
                                    .putWord(30) // Location
                                    .putWord(lightLocation)
                                    .build()
                            )

                            val output = code.addStaticVar(3, vec2, "VibrancyFragmentLight")

                            code.addEntrypointVars(input.id, output.id)

                            code.insert(
                                code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                    .putWord(output.id)
                                    .putWord(30) // Location
                                    .putWord(lightUVLocation)
                                    .build()
                            )

                            val tempVar = code.bound++
                            code.insert(
                                code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                    .putWord(vec2)
                                    .putWord(tempVar)
                                    .putWord(input.id)
                                    .build(),
                                ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                    .putWord(output.id)
                                    .putWord(tempVar)
                                    .build()
                            )
                        }

                        val texCoordLocation = mapper.get("VibrancyVertexTexCoord")
                        val sodiumTexCoord = code.findVariable(name = "v_TexCoord")

                        if (texCoordLocation != null || sodiumTexCoord != null) {
                            val sampler0Var = code.findVariable(
                                name = "Sampler0"
                            ) ?: if (key.program.loader == ShaderLoaderType.SODIUM) code.findVariable(name = "u_BlockTex") else null

                            if (sampler0Var != null) {
                                val vec4 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 4).findOrInject(code)
                                val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(code)

                                val input: ShaderVariable

                                if (sodiumTexCoord != null) {
                                    input = sodiumTexCoord
                                } else {
                                    input = code.addStaticVar(1, vec2, "VibrancyVertexTexCoord")

                                    code.insert(
                                        code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                        ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                            .putWord(input.id)
                                            .putWord(30) // Location
                                            .putWord(texCoordLocation)
                                            .build()
                                    )
                                }

                                val output = code.addStaticVar(3, vec4, "VibrancyFragmentAlbedo")

                                code.addEntrypointVars(input.id, output.id)

                                code.insert(
                                    code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                    ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                        .putWord(output.id)
                                        .putWord(30) // Location
                                        .putWord(albedoLocation)
                                        .build()
                                )

                                val vertexColorLocation = mapper.get("VibrancyVertexColor")

                                if (vertexColorLocation != null) {
                                    val vertexColor = code.addStaticVar(1, vec4, "VibrancyVertexColor")

                                    code.insert(
                                        code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                        ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                            .putWord(vertexColor.id)
                                            .putWord(30) // Location
                                            .putWord(vertexColorLocation)
                                            .build()
                                    )

                                    val tempSamplerVar = code.bound++
                                    val tempTexCoordVar = code.bound++
                                    val tempColorVar = code.bound++
                                    val tempPreResultVar = code.bound++
                                    val tempResultVar = code.bound++
                                    code.insert(
                                        code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                        ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                            .putWord(sampler0Var.type)
                                            .putWord(tempSamplerVar)
                                            .putWord(sampler0Var.id)
                                            .build(),
                                        ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                            .putWord(vertexColor.type)
                                            .putWord(tempColorVar)
                                            .putWord(vertexColor.id)
                                            .build(),
                                        ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                            .putWord(vec4)
                                            .putWord(tempTexCoordVar)
                                            .putWord(input.id)
                                            .build(),

                                        ShaderOpcode.Builder(ShaderOpcode.OP_IMAGE_SAMPLE_IMPLICIT_LOD)
                                            .putWord(vec4)
                                            .putWord(tempPreResultVar)
                                            .putWord(tempSamplerVar)
                                            .putWord(tempTexCoordVar)
                                            .build(),
                                        ShaderOpcode.Builder(ShaderOpcode.OP_F_MUL)
                                            .putWord(vec4)
                                            .putWord(tempResultVar)
                                            .putWord(tempPreResultVar)
                                            .putWord(tempColorVar)
                                            .build(),

                                        ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                            .putWord(output.id)
                                            .putWord(tempResultVar)
                                            .build()
                                    )
                                } else {
                                    val tempSamplerVar = code.bound++
                                    val tempTexCoordVar = code.bound++
                                    val tempResultVar = code.bound++
                                    code.insert(
                                        code.findOpcodeInMethod("main", ShaderOpcode.OP_RETURN)!!.index,
                                        ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                            .putWord(sampler0Var.type)
                                            .putWord(tempSamplerVar)
                                            .putWord(sampler0Var.id)
                                            .build(),
                                        ShaderOpcode.Builder(ShaderOpcode.OP_LOAD)
                                            .putWord(vec4)
                                            .putWord(tempTexCoordVar)
                                            .putWord(input.id)
                                            .build(),

                                        ShaderOpcode.Builder(ShaderOpcode.OP_IMAGE_SAMPLE_IMPLICIT_LOD)
                                            .putWord(vec4)
                                            .putWord(tempResultVar)
                                            .putWord(tempSamplerVar)
                                            .putWord(tempTexCoordVar)
                                            .build(),

                                        ShaderOpcode.Builder(ShaderOpcode.OP_STORE)
                                            .putWord(output.id)
                                            .putWord(tempResultVar)
                                            .build()
                                    )
                                }
                            }
                        }
                    }

                    else -> {}
                }

                return code
            }
        }
    }
}