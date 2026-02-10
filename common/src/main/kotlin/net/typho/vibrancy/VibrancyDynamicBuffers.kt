package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.shaders.ShaderProgramKey
import net.typho.big_shot_lib.api.shaders.ShaderSourceKey
import net.typho.big_shot_lib.api.shaders.ShaderSourceType
import net.typho.big_shot_lib.api.shaders.mixins.ShaderBytecodeBuffer
import net.typho.big_shot_lib.api.shaders.mixins.ShaderMixin
import net.typho.big_shot_lib.api.shaders.mixins.ShaderOpcode
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

    override fun create(key: ShaderProgramKey): ShaderMixin {
        return if (key.loader.autoBindLocations) {
        }
    }

    class MinecraftImpl : ShaderMixin {
    }

    override fun mixinBytecode(key: ShaderSourceKey, code: ShaderBytecodeBuffer): ShaderBytecodeBuffer {
        if (exclude.contains(key.program.location) || key.program.location.namespace == Vibrancy.MOD_ID) {
            return code
        }

        if (builtin.contains(key.program.location)) {
            if (key.type == ShaderSourceType.FRAGMENT) {
                code.findVariable(name = "VibrancyFragmentNormal")?.let { normal ->
                    code.findOpcode(ShaderOpcode.OP_DECORATE, 0 to normal.id, 1 to 30)?.putWord(2, normalsLocation)
                }
                code.findVariable(name = "VibrancyFragmentLight")?.let { light ->
                    code.findOpcode(ShaderOpcode.OP_DECORATE, 0 to light.id, 1 to 30)?.putWord(2, lightUVLocation)
                }
                code.findVariable(name = "VibrancyFragmentAlbedo")?.let { albedo ->
                    code.findOpcode(ShaderOpcode.OP_DECORATE, 0 to albedo.id, 1 to 30)?.putWord(2, albedoLocation)
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
                if (key.program.format.contains(VertexFormatElement.NORMAL) || shader.namespace == "sodium") {
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
                                val vec3 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 3).findOrInject(context)
                                val input = context.addStaticVar(1, vec3, "VibrancyInputNormal")
                                context.addEntrypointVars(input.id)

                                val location = locations.getMapper(1, type)!!.map("VibrancyInputNormal", 1)
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

                        val location = locations.getMapper(3, type)!!.map("VibrancyVertexNormal", 1)
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

                if (format.contains(VertexFormatElement.UV2) || shader.namespace == "sodium") {
                    val lightVar = if (shader.namespace == "sodium") {
                        context.locateVariable(
                            name = "_vert_tex_light_coord"
                        )
                    } else {
                        context.locateVariable(
                            name = format.getElementName(VertexFormatElement.UV2)
                        )
                    }

                    if (lightVar != null) {
                        val floatType = ShaderPrimitiveType.FLOAT_32.findOrInject(context)
                        val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(context)

                        val output = context.addStaticVar(3, vec2, "VibrancyVertexLight")
                        context.addEntrypointVars(output.id)

                        val location = locations.getMapper(3, type)!!.map("VibrancyVertexLight", 1)
                        context.inject(
                            context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                            Opcode.Builder(Opcode.OP_DECORATE)
                                .putInt(output.id)
                                .putInt(30) // Location
                                .putInt(location)
                                .build()
                        )

                        val tempVar = context.bound++

                        if (shader.namespace == "sodium") {
                            context.putBound()

                            context.inject(
                                context.locateOpcodeInMethod(Opcode.OP_RETURN, "main")!!.index,
                                Opcode.Builder(Opcode.OP_LOAD)
                                    .putInt(vec2)
                                    .putInt(tempVar)
                                    .putInt(lightVar.id)
                                    .build(),
                                Opcode.Builder(Opcode.OP_STORE)
                                    .putInt(output.id)
                                    .putInt(tempVar)
                                    .build()
                            )
                        } else {
                            val constantVar = context.bound++
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
                }

                if (format.contains(VertexFormatElement.UV0)) {
                    val uvVar: ShaderVariable? = context.locateVariable(
                        name = format.getElementName(VertexFormatElement.UV0)
                    )

                    if (uvVar != null) {
                        val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(context)

                        val output = context.addStaticVar(3, vec2, "VibrancyVertexTexCoord")
                        context.addEntrypointVars(output.id)

                        val location = locations.getMapper(3, type)!!.map("VibrancyVertexTexCoord", 1)
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

                        if (format.contains(VertexFormatElement.COLOR) && !noVertexColor.contains(shader)) {
                            val colorVar = context.locateVariable(
                                name = format.getElementName(VertexFormatElement.COLOR)
                            )

                            if (colorVar != null) {
                                val vec4 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 4).findOrInject(context)

                                val colorOutput = context.addStaticVar(3, vec4, "VibrancyVertexColor")
                                context.addEntrypointVars(colorOutput.id)

                                val colorLocation = locations.getMapper(3, type)!!.map("VibrancyVertexColor", 1)
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
            ShaderSourceType.FRAGMENT -> {
                val mapper = locations.getMapper(1, type)!!
                val normalLocation = mapper.map.get("VibrancyVertexNormal")?.location

                if (normalLocation != null) {
                    val vec3 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 3).findOrInject(context)

                    val input = context.addStaticVar(1, vec3, "VibrancyVertexNormal")

                    context.inject(
                        context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                        Opcode.Builder(Opcode.OP_DECORATE)
                            .putInt(input.id)
                            .putInt(30) // Location
                            .putInt(normalLocation)
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

                val lightLocation = mapper.map.get("VibrancyVertexLight")?.location

                if (lightLocation != null) {
                    val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(context)

                    val input = context.addStaticVar(1, vec2, "VibrancyVertexLight")

                    context.inject(
                        context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                        Opcode.Builder(Opcode.OP_DECORATE)
                            .putInt(input.id)
                            .putInt(30) // Location
                            .putInt(lightLocation)
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

                val texCoordLocation = mapper.map.get("VibrancyVertexTexCoord")?.location
                val sodiumTexCoord = context.locateVariable(name = "v_TexCoord")

                if (texCoordLocation != null || sodiumTexCoord != null) {
                    val sampler0Var = context.locateVariable(
                        name = "Sampler0"
                    ) ?: if (shader.namespace == "sodium") context.locateVariable(name = "u_BlockTex") else null

                    if (sampler0Var != null) {
                        val vec4 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 4).findOrInject(context)
                        val vec2 = ShaderVectorType(ShaderPrimitiveType.FLOAT_32, 2).findOrInject(context)

                        val input: ShaderVariable

                        if (sodiumTexCoord != null) {
                            input = sodiumTexCoord
                        } else {
                            input = context.addStaticVar(1, vec2, "VibrancyVertexTexCoord")

                            context.inject(
                                context.locateOpcode(Opcode.OP_DECORATE)!!.index,
                                Opcode.Builder(Opcode.OP_DECORATE)
                                    .putInt(input.id)
                                    .putInt(30) // Location
                                    .putInt(texCoordLocation)
                                    .build()
                            )
                        }

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

                        val vertexColorLocation = mapper.map.get("VibrancyVertexColor")?.location

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
            else -> {}
        }

        return code
    }
}