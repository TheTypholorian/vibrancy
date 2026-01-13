package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.BigShotLib.cube
import net.typho.big_shot_lib.api.impl.NeoIndexedBuffer
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.resource.BufferUsage
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.big_shot_lib.gl.state.BlendFactor
import net.typho.big_shot_lib.gl.state.BlendFunction
import net.typho.big_shot_lib.gl.state.CullFace
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.RenderingBlockLight
import net.typho.vibrancy.util.StateFunction
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLight> {
    @JvmField
    val meshes = HashMap<ChunkPos, Chunk>()
    @JvmField
    var dirty = true

    override fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<SubtleLightInfo> {
        return RecordCodecBuilder.mapCodec {
            it.group(
                StateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .fieldOf("color")
                    .forGetter { info -> info.color },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness },
                StateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .optionalFieldOf("offset", StateFunction(Vector3f(0.5f)))
                    .forGetter { info -> info.offset },
                StateFunction.codec(Codec.BOOL, stateDefinition)
                    .optionalFieldOf("enabled", StateFunction(true))
                    .forGetter { info -> info.enabled }
            ).apply(it, ::SubtleLightInfo)
        }
    }

    override fun render(manager: LightManager, lights: Set<RenderingBlockLight<SubtleLight>>) {
        if (dirty) {
            val map = HashMap<ChunkPos, MutableSet<RenderingBlockLight<SubtleLight>>>()

            for (light in lights) {
                if (light.light.shouldRender(manager)) {
                    map.computeIfAbsent(ChunkPos(light.light.pos)) { HashSet() }.add(light)
                }
            }

            meshes.entries.removeIf { entry ->
                if (!map.containsKey(entry.key)) {
                    entry.value.free()
                    return@removeIf true
                } else {
                    return@removeIf false
                }
            }

            for (entry in map) {
                val chunk = meshes.computeIfAbsent(entry.key) { Chunk() }

                val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
                val buffer = MemoryUtil.memAllocFloat(8 * entry.value.size)

                var box: AABB? = null

                for (light in entry.value) {
                    box = if (box == null) {
                        light.light.getBoundingBox()
                    } else {
                        box.intersect(light.light.getBoundingBox())
                    }

                    builder.cube(light.light.getBoundingBox())

                    val color = light.light.color
                    val pos = light.light.getAbsolutePos()

                    buffer.put(color.x).put(color.y).put(color.z).put(0f)
                    buffer.put(pos.x).put(pos.y).put(pos.z).put(0f)
                }

                chunk.box = box

                chunk.vbo.bind()
                chunk.vbo.upload(builder.build()!!)
                VertexBuffer.unbind()

                chunk.ssbo.bind()
                chunk.ssbo.upload(MemoryUtil.memByteBuffer(buffer))
                chunk.ssbo.unbind()

                MemoryUtil.memFree(buffer)
            }

            dirty = false
        }

        GlStack().use { stack ->
            stack.disable(GlCapability.DEPTH_TEST)
            stack.disable(GlCapability.STENCIL_TEST)
            stack.enable(GlCapability.CULL_FACE)
            stack.set(CullFace.FRONT)
            stack.enable(GlCapability.BLEND)
            stack.set(
                BlendFunction(
                    BlendFactor.ONE,
                    BlendFactor.ONE
                )
            )

            val boxShader = NeoShader.get(Vibrancy.id("subtle_box"))!!

            boxShader.bind(stack)
            boxShader.setCommonUniforms(modelViewMat = manager.getViewMatrix())

            boxShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
            boxShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

            boxShader.getUniform("CameraPos")?.set(Vibrancy.camera)
            boxShader.getUniform("LightRadius")?.set(4f)

            boxShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

            for (entry in meshes) {
                entry.value.render(manager, stack)
            }

            VertexBuffer.unbind()
        }
    }

    data class Chunk(
        val vbo: VertexBuffer = VertexBuffer(VertexBuffer.Usage.STATIC),
        val ssbo: NeoIndexedBuffer = NeoIndexedBuffer(null, GlResourceType.UNIFORM_BUFFER, BufferUsage.STATIC_DRAW),
        var box: AABB? = null
    ) : NativeResource {
        fun render(manager: LightManager, stack: GlStack) {
            if (manager.getCullingFrustum().isVisible(box!!)) {
                ssbo.bindBase(stack, 0)

                vbo.bind()
                vbo.draw()
            }
        }

        override fun free() {
            vbo.close()
            ssbo.release()
        }
    }
}