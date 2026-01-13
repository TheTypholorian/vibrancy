package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.BigShotLib.cube
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.IntAction
import net.typho.big_shot_lib.gl.state.StencilOp
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLight
import net.typho.vibrancy.block.RenderingBlockLight
import org.joml.Matrix4f
import org.joml.Vector3f

class SubtleLight(
    val color: Vector3f,
    val offset: Vector3f,
    val pos: BlockPos
) : BlockLight<SubtleLightInfo> {
    val box by lazy {
        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.cube(getBoundingBox())

        val vbo = VertexBuffer(VertexBuffer.Usage.STATIC)

        vbo.bind()
        vbo.upload(builder.build()!!)
        VertexBuffer.unbind()

        return@lazy vbo
    }

    constructor(info: SubtleLightInfo, state: StateHolder<*, *>, pos: BlockPos) : this(
        info.color.apply(state).mul(info.brightness.apply(state), Vector3f()),
        info.offset.apply(state),
        pos
    )

    override fun getBoundingBox(): AABB = AABB.ofSize(
        Vec3(getAbsolutePos()),
        8.0,
        8.0,
        8.0
    )

    override fun rebuildShadows(manager: LightManager) {
    }

    override fun getType() = SubtleLightType

    override fun shouldRaytrace() = false

    override fun numShadows() = 0

    override fun numAsyncTasksActive() = 0

    override fun free() {
        box.close()
    }

    override fun getBlockPos() = pos

    override fun getAbsolutePos(): Vector3f {
        return Vector3f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()).add(offset)
    }

    override fun getShadowBox() = null

    override fun getShadowPredicate() = null

    fun render(manager: LightManager, rendering: RenderingBlockLight<SubtleLight>, stack: GlStack) {
        val boxShader = NeoShader.get(Vibrancy.id("subtle_box"))!!

        boxShader.bind(stack)
        boxShader.setCommonUniforms(modelViewMat = manager.getViewMatrix())

        boxShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
        boxShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

        boxShader.getUniform("LightPos")?.set(getAbsolutePos())
        boxShader.getUniform("LightColor")?.set(color)
        boxShader.getUniform("LightRadius")?.set(4f)
        boxShader.getUniform("CameraPos")?.set(Vibrancy.camera)

        boxShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

        stack.set(
            StencilOp(
                IntAction.KEEP,
                IntAction.KEEP,
                IntAction.KEEP,
            )
        )

        box.bind()
        box.draw()
        VertexBuffer.unbind()
    }
}