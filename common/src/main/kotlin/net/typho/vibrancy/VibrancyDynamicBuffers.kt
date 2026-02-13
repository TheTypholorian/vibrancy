package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.renderer.RenderType
import net.typho.big_shot_lib.api.client.rendering.shaders.ShaderLoaderType
import net.typho.big_shot_lib.api.client.rendering.shaders.ShaderProgramKey
import net.typho.big_shot_lib.api.client.rendering.shaders.ShaderSourceKey
import net.typho.big_shot_lib.api.client.rendering.shaders.ShaderSourceType
import net.typho.big_shot_lib.api.client.rendering.shaders.mixins.*
import net.typho.big_shot_lib.api.client.rendering.shaders.variables.ShaderVariable
import net.typho.big_shot_lib.api.client.rendering.shaders.variables.ShaderVariableType
import net.typho.big_shot_lib.api.client.rendering.textures.NeoTexture2D
import net.typho.big_shot_lib.api.client.rendering.textures.TextureFormat
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import org.jetbrains.annotations.ApiStatus
import org.lwjgl.opengl.GL11.GL_NONE
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL30.*

object VibrancyDynamicBuffers : ShaderMixin.Factory {
    private var initialized = false

    @JvmField
    var normalLocation: Int = 0

    @JvmField
    var normalTexture: NeoTexture2D? = null

    @JvmField
    var albedoLocation: Int = 0

    @JvmField
    var albedoTexture: NeoTexture2D? = null

    @JvmField
    var lightUVLocation: Int = 0

    @JvmField
    var lightUVTexture: NeoTexture2D? = null

    @JvmField
    val noVertexColor = HashSet<ResourceIdentifier>(
        RenderType.chunkBufferLayers()
            .filterIsInstance<RenderType.CompositeRenderType>()
            .mapNotNull { type ->
                type.state().shaderState.shader.map { shader ->
                    shader.get()?.name
                }.orElse(null)
            }
            .map { name -> ResourceIdentifier(name) }
    )
    @JvmField
    val exclude = HashSet<ResourceIdentifier>(listOf(
        ResourceIdentifier("rendertype_lines"),
        ResourceIdentifier("particle")
    ))
    @JvmField
    val builtin = HashSet<ResourceIdentifier>(listOf(
        ResourceIdentifier("rendertype_end_portal")
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
        normalLocation = pickAvailableAttachment()!!
        normalTexture = NeoTexture2D(TextureFormat.RGB16_SNORM)

        albedoLocation = pickAvailableAttachment(normalLocation)!!
        albedoTexture = NeoTexture2D(TextureFormat.RGB)

        lightUVLocation = pickAvailableAttachment(normalLocation, albedoLocation)!!
        lightUVTexture = NeoTexture2D(TextureFormat.RG)

        attach(width, height)

        initialized = true
    }

    @ApiStatus.Internal
    @JvmStatic
    fun attach(width: Int, height: Int) {
        normalTexture!!.bind()
        normalTexture!!.resize(width, height).uploadNull()
        normalTexture!!.attachToFramebuffer(GL_COLOR_ATTACHMENT0 + normalLocation)
        normalTexture!!.unbind()

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
                GL_COLOR_ATTACHMENT0 + normalLocation,
                GL_COLOR_ATTACHMENT0 + albedoLocation,
                GL_COLOR_ATTACHMENT0 + lightUVLocation
            )
        )
        enableBlend()
    }

    @ApiStatus.Internal
    @JvmStatic
    fun enableBlend() {
        glDisablei(GL_BLEND, normalLocation)
        glEnablei(GL_BLEND, albedoLocation)
        glDisablei(GL_BLEND, lightUVLocation)
    }

    @ApiStatus.Internal
    @JvmStatic
    fun disableBlend() {
        glDisablei(GL_BLEND, normalLocation)
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
            override fun mixinPostCompile(key: ShaderSourceKey, code: String): String {
                return code.replace("#version 450", "#version 430")
            }

            override fun mixinBytecode(key: ShaderSourceKey, code: ShaderBytecodeBuffer): ShaderBytecodeBuffer {
                if (exclude.contains(key.program.location) || key.program.location.namespace == Vibrancy.MOD_ID) {
                    return code
                }

                if (builtin.contains(key.program.location)) {
                    if (key.type == ShaderSourceType.FRAGMENT) {
                        code.findVariable(name = "VibrancyFragmentNormal")?.let { normal ->
                            code.findOpcode(ShaderOpcode.OP_DECORATE, 0 to normal.id, 1 to 30)
                                ?.putWord(2, normalLocation)
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
                        if (key.program.format.contains(VertexFormatElement.NORMAL) || key.program.location.equals("sodium", "blocks/block_layer_opaque")) {
                            val normalVar: ShaderVariable?

                            if (key.program.location.equals("sodium", "blocks/block_layer_opaque")) {
                                val vec3 = ShaderVariableType.FLOAT_VEC3.findOrInjectBytecode(code)

                                val input = code.addStaticVar(ShaderStorageClass.INPUT, vec3, "VibrancyInputNormal")
                                code.addEntrypointVars(input.id)

                                val location = locations.getMapper(ShaderStorageClass.OUTPUT, key.type)!!.map(1, "VibrancyInputNormal")
                                code.insert(
                                    code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                    ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                        .putWord(input.id)
                                        .putWord(30) // Location
                                        .putWord(location)
                                        .build()
                                )

                                normalVar = input
                            } else {
                                normalVar = code.findVariable(
                                    name = key.program.format.getElementName(VertexFormatElement.NORMAL)
                                )
                            }

                            if (normalVar != null) {
                                val output = code.addStaticVar(ShaderStorageClass.OUTPUT, normalVar.type, "VibrancyVertexNormal")
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
                                val floatType = ShaderVariableType.FLOAT.findOrInjectBytecode(code)
                                val vec2 = ShaderVariableType.FLOAT_VEC2.findOrInjectBytecode(code)

                                val output = code.addStaticVar(ShaderStorageClass.OUTPUT, vec2, "VibrancyVertexLight")
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
                                val vec2 = ShaderVariableType.FLOAT_VEC2.findOrInjectBytecode(code)

                                val output = code.addStaticVar(ShaderStorageClass.OUTPUT, vec2, "VibrancyVertexTexCoord")
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
                                        val vec4 = ShaderVariableType.FLOAT_VEC4.findOrInjectBytecode(code)

                                        val colorOutput = code.addStaticVar(ShaderStorageClass.OUTPUT, vec4, "VibrancyVertexColor")
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
                            val vec3 = ShaderVariableType.FLOAT_VEC3.findOrInjectBytecode(code)

                            val input = code.addStaticVar(ShaderStorageClass.INPUT, vec3, "VibrancyVertexNormal")

                            code.insert(
                                code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                    .putWord(input.id)
                                    .putWord(30) // Location
                                    .putWord(normalLocation)
                                    .build()
                            )

                            val output = code.addStaticVar(ShaderStorageClass.OUTPUT, vec3, "VibrancyFragmentNormal")

                            code.addEntrypointVars(input.id, output.id)

                            code.insert(
                                code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                    .putWord(output.id)
                                    .putWord(30) // Location
                                    .putWord(VibrancyDynamicBuffers.normalLocation)
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
                            val vec2 = ShaderVariableType.FLOAT_VEC2.findOrInjectBytecode(code)

                            val input = code.addStaticVar(ShaderStorageClass.INPUT, vec2, "VibrancyVertexLight")

                            code.insert(
                                code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                    .putWord(input.id)
                                    .putWord(30) // Location
                                    .putWord(lightLocation)
                                    .build()
                            )

                            val output = code.addStaticVar(ShaderStorageClass.OUTPUT, vec2, "VibrancyFragmentLight")

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
                                val vec4 = ShaderVariableType.FLOAT_VEC4.findOrInjectBytecode(code)
                                val vec2 = ShaderVariableType.FLOAT_VEC2.findOrInjectBytecode(code)

                                val input: ShaderVariable

                                if (sodiumTexCoord != null) {
                                    input = sodiumTexCoord
                                } else {
                                    input = code.addStaticVar(ShaderStorageClass.INPUT, vec2, "VibrancyVertexTexCoord")

                                    code.insert(
                                        code.findOpcode(ShaderOpcode.OP_DECORATE)!!.index,
                                        ShaderOpcode.Builder(ShaderOpcode.OP_DECORATE)
                                            .putWord(input.id)
                                            .putWord(30) // Location
                                            .putWord(texCoordLocation)
                                            .build()
                                    )
                                }

                                val output = code.addStaticVar(ShaderStorageClass.OUTPUT, vec4, "VibrancyFragmentAlbedo")

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
                                    val vertexColor = code.addStaticVar(ShaderStorageClass.INPUT, vec4, "VibrancyVertexColor")

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