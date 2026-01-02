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
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyDynamicBuffers
import net.typho.vibrancy.shadows.PointShadowManager
import net.typho.vibrancy.util.boxOfRadius
import net.typho.vibrancy.util.expand
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
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

    override fun render(manager: LightManager, raytrace: Boolean, stack: GlStack) {
        val shadowRadius = getShadowRadius(manager)
        val shadowRadiusSq = shadowRadius * shadowRadius

        for (pos in manager.dirtyBlocks) {
            if (manager.getLevel().dimension()
                    .equals(pos.dimension()) && pos.pos.distToCenterSqr(Vec3(getPosition())) < shadowRadiusSq
            ) {
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

        val shadowShader = NeoShader.get(Vibrancy.id("point_shadow"))!!
        shadowShader.bind(stack)
        shadowShader.setCommonUniforms(modelViewMat = manager.viewMatrix!!)

        shadowShader.getUniform("IProjMat")?.set(Vibrancy.iProjMat)
        shadowShader.getUniform("IModelMat")?.set(Vibrancy.iModelMat)
        shadowShader.getUniform("CameraPos")?.set(Vibrancy.camera)

        glStencilFunc(
            GL_NOTEQUAL,
            LightManager.Companion.SHADOW_MASK,
            LightManager.Companion.BLOCK_STENCIL_MASK or LightManager.Companion.SHADOW_MASK
        )
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)

        shadows.render(manager, raytrace, this, shadowShader, stack)

        val boxShader = NeoShader.get(Vibrancy.id("point_box"))!!

        boxShader.bind(stack)
        boxShader.setCommonUniforms(modelViewMat = manager.viewMatrix!!)

        boxShader.getUniform("LightPos")?.set(getPosition())
        val color = getColor()
        boxShader.getUniform("LightColor")?.set(Vector3f(color.red / 255f, color.green / 255f, color.blue / 255f))
        boxShader.getUniform("LightRadius")?.set(getRadius())
        boxShader.getUniform("CameraPos")?.set(Vibrancy.camera)

        boxShader.getUniform("IProjMat")?.set(Vibrancy.iProjMat)
        boxShader.getUniform("IModelMat")?.set(Vibrancy.iModelMat)
        boxShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)
        boxShader.setSampler("VibrancyNormalSampler", VibrancyDynamicBuffers.normalsTexture!!)

        stack.enable(GlCapability.STENCIL_TEST) // TODO
        glStencilFunc(GL_EQUAL, 0, LightManager.Companion.SHADOW_MASK)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)

        boxMesh.bind()
        boxMesh.draw()
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