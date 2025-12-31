package net.typho.vibrancy.light

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.BigShotLib.cube
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.ComparisonMode
import net.typho.big_shot_lib.gl.state.IntAction
import net.typho.big_shot_lib.gl.state.StencilFunc
import net.typho.big_shot_lib.gl.state.StencilOp
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyDynamicBuffers
import net.typho.vibrancy.shadows.PointShadowManager
import net.typho.vibrancy.util.boxOfRadius
import net.typho.vibrancy.util.expand
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.system.NativeResource
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor

abstract class PointLight : Light, NativeResource {
    val boxMesh = VertexBuffer(if (isStatic()) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC)
    val shadows = PointShadowManager(isStatic())
    var boxDirty = true
    var shadowsDirty = true

    override fun free() {
        boxMesh.close()
        shadows.free()
    }

    protected fun uploadBoxMesh() {
        val builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
        builder.cube(getBoundingBox())

        boxMesh.bind()
        boxMesh.upload(builder.buildOrThrow())
        VertexBuffer.unbind()
    }

    fun renderMesh(vbo: VertexBuffer, view: Matrix4f, shader: IShader) {
        val color = getColor()

        shader.setCommonUniforms(modelViewMat = view)
        shader.getUniform("LightPos")?.set(getPosition())
        shader.getUniform("LightColor")?.set(Vector3f(color.red / 255f, color.green / 255f, color.blue / 255f))
        shader.getUniform("LightRadius")?.set(getRadius())

        vbo.bind()
        vbo.draw()
    }

    override fun render(manager: LightManager, raytrace: Boolean, stack: GlStack) {
        val shadowRadius = getShadowRadius(manager)
        val shadowRadiusSq = shadowRadius * shadowRadius

        for (pos in manager.dirtyBlocks) {
            if (manager.getLevel().dimension().equals(pos.dimension()) && pos.pos.distToCenterSqr(Vec3(getPosition())) < shadowRadiusSq) {
                shadows.rebuildBlock(manager, pos.pos, this)
            }
        }

        if (boxDirty) {
            uploadBoxMesh()

            boxDirty = false
        }

        if (shadowsDirty && raytrace) {
            shadows.fullRebuildAsync(manager, getShadowBox(), this)

            shadowsDirty = false
        }

        glClear(GL_STENCIL_BUFFER_BIT)

        NeoShader.get(Vibrancy.id("point_shadow"))!!.bind().use {
            stack.set(StencilFunc(
                ComparisonMode.NOTEQUAL, 
                LightManager.SHADOW_MASK, 
                LightManager.BLOCK_STENCIL_MASK or LightManager.SHADOW_MASK
            ))
            stack.set(StencilOp(IntAction.KEEP, IntAction.KEEP, IntAction.REPLACE))

            shadows.render(manager, raytrace, this, it.resource(), stack)
        }

        val shader = NeoShader.get(Vibrancy.id("point_box"))!!
        shader.bind(stack)

        val cameraPos = manager.getCamera().position
        shader.getUniform("CameraPos")?.set(cameraPos.x.toFloat(), cameraPos.y.toFloat(), cameraPos.z.toFloat())
        shader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)
        shader.setSampler("VibrancyNormalSampler", VibrancyDynamicBuffers.normalsTexture!!)

        stack.set(StencilFunc(ComparisonMode.EQUAL, 0, LightManager.SHADOW_MASK))
        stack.set(StencilOp(IntAction.KEEP, IntAction.KEEP, IntAction.KEEP))

        renderMesh(boxMesh, manager.viewMatrix!!, shader)
    }

    override fun getCullingBox() = getBoundingBox()

    fun getBoundingBox() = boxOfRadius(getPosition(), getRadius())

    fun getShadowBox() = BlockBox.of(
        BlockPos(
            floor(getPosition().x.toDouble()).toInt(),
            floor(getPosition().y.toDouble()).toInt(),
            floor(getPosition().z.toDouble()).toInt()
        )
    ).expand(ceil(getRadius()).toInt())

    abstract fun getPosition(): Vector3f

    abstract fun getBlockPos(): BlockPos

    abstract fun getRadius(): Float

    abstract fun getShadowRadius(manager: LightManager): Int

    abstract fun getColor(): Color

    protected fun isStatic() = true
}